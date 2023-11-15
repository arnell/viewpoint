package com.gregarnell.viewpoint.rest;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.MetadataException;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.gregarnell.viewpoint.ViewpointProperties;
import com.gregarnell.viewpoint.entity.Image;
import com.gregarnell.viewpoint.entity.Segment;
import com.gregarnell.viewpoint.repo.ImageRepo;
import com.gregarnell.viewpoint.repo.SegmentRepo;
import com.gregarnell.viewpoint.rest.dto.ImageDto;
import com.gregarnell.viewpoint.rest.dto.SegmentDto;
import com.gregarnell.viewpoint.rest.exception.NotFoundException;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.imgscalr.Scalr;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

@RestController
@RequestMapping("image")
public class ImageRestController {

    @Autowired
    ImageRepo imageRepo;

    @Autowired
    SegmentRepo segmentRepo;

    @Autowired
    ViewpointProperties viewpointProperties;

    //http://localhost:8080/image/query
    @GetMapping("query")
    public List<ImageDto> query(@RequestParam LocalDate startDate, @RequestParam LocalDate endDate) {
        if (startDate == null && endDate == null) {
            List<ImageDto> imageList = new ArrayList<>();
            for (Image image : imageRepo.findAll()) {
                imageList.add(new ImageDto(image.getId(), image.getName(), "/image/" + image.getName(), image.getRatio()));
            }
            return imageList;
        }
        return imageRepo.findByTakenBetweenOrderByTakenDesc(startDate, endDate).stream()
                .map(image -> new ImageDto(image.getId(), image.getName(), "/image/" + image.getName(), image.getRatio()))
                .toList();
    }

    @GetMapping("segment/query")
    public List<SegmentDto> querySegments() {
        return segmentRepo.findByOrderByStartDateDesc().stream()
                .map(s -> new SegmentDto(s.getId(), s.getStartDate(), s.getEndDate(), s.getCount()))
                .toList();
    }

    //todo: how should i really be serving images?
    //todo: add caching header
    @GetMapping("{filename}")
    public ResponseEntity<byte[]> getImage(@PathVariable("filename") String filename) {
        File file = new File(viewpointProperties.getImagesRoot() + "/thumb/" + filename);
        if (!file.exists()) {
            throw new NotFoundException();
        }
        try {
            byte[] image = FileUtils.readFileToByteArray(file);
            return ResponseEntity.ok().contentType(MediaType.IMAGE_JPEG).body(image);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    @GetMapping("makethumb")
    public void thumb() throws IOException, ImageProcessingException, NoSuchAlgorithmException, MetadataException {
        imageRepo.deleteAll();
        List<File> files = Stream.of(new File(viewpointProperties.getImagesRoot()).listFiles())
                .filter(file -> !file.isDirectory())
                .toList();

        for (File file : files) {
            BufferedImage src = ImageIO.read(file);
            if (src == null) {
                System.out.println(file.getName());
                continue;
            }
            Metadata metadata = ImageMetadataReader.readMetadata(new FileInputStream(file));
            Image image = new Image();

            ExifIFD0Directory firstDirectoryOfType = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            if (firstDirectoryOfType != null && firstDirectoryOfType.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
                int orientation = firstDirectoryOfType.getInt(ExifIFD0Directory.TAG_ORIENTATION);
                image.setOrientation(orientation);
                Scalr.Rotation rotation = switch (orientation) {
                    case 6 -> // Right side, top (Rotate 90 CW)
                            Scalr.Rotation.CW_90;
                    case 3 -> // Bottom, right side (Rotate 180)
                            Scalr.Rotation.CW_180;
                    case 8 -> // Left side, bottom (Rotate 270 CW)
                            Scalr.Rotation.CW_270;
                    default -> null;
                };
                if (rotation != null) {
                    src = Scalr.rotate(src, rotation, Scalr.OP_ANTIALIAS);
                }
            }
            BufferedImage resized = Scalr.resize(src, 800);
            File outputFile = new File(viewpointProperties.getImagesRoot() + "/thumb/" + file.getName());

            FileUtils.createParentDirectories(outputFile);
            Pair<String, String> checksum = checksum(file);

            ImageIO.write(resized, "jpg", outputFile);
            image.setChecksumMd5(checksum.getLeft());
            image.setChecksumSha(checksum.getRight());
            image.setName(file.getName());
            image.setPath(file.getAbsolutePath());
            image.setThumbPath(viewpointProperties.getImagesRoot() + "/thumb/" + file.getName());
            image.setWidth(src.getWidth());
            image.setHeight(src.getHeight());
            image.setRatio(BigDecimal.valueOf(src.getWidth()).setScale(2, RoundingMode.HALF_UP)
                    .divide(BigDecimal.valueOf(src.getHeight()), RoundingMode.HALF_UP));
            Directory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            Date date = directory.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            image.setTaken(date.toInstant()
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDate());
            imageRepo.save(image);
        }
        segmentize();
    }

    @GetMapping("test")
    public void testSingle(@RequestParam("filePath") String filePath) throws IOException, ImageProcessingException {
        File file = new File(filePath);
        Metadata metadata = ImageMetadataReader.readMetadata(new FileInputStream(file));
        BufferedImage orig = ImageIO.read(file);
        BufferedImage resized = Scalr.resize(orig, 800);
        File outputFile = new File(viewpointProperties.getImagesRoot() + "/test/" + file.getName());
        ImageIO.write(resized, "jpg", outputFile);
        metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class).getString(ExifSubIFDDirectory.TAG_SCENE_TYPE);
    }

    private Pair<String, String> checksum(File file) throws NoSuchAlgorithmException {
        MessageDigest md5 = MessageDigest.getInstance("MD5");
        MessageDigest sha = MessageDigest.getInstance("SHA-256");
        try (InputStream is
                     = new DigestInputStream(
                             new DigestInputStream(
                                     new FileInputStream(file), md5), sha)) {
            IOUtils.consume(is);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String md5Checksum = new BigInteger(1, md5.digest()).toString(16);
        String shaChecksum = new BigInteger(1, sha.digest()).toString(16);
        return Pair.of(md5Checksum, shaChecksum);
    }

    @GetMapping("segmentize")
    public void segmentize() {
        segmentRepo.deleteAll();
        Map<YearMonth, Segment> segments = new HashMap<>();
        Iterable<Image> all = imageRepo.findAll();
        for (Image image : all) {
            LocalDate taken = image.getTaken();
            YearMonth yearMonth = YearMonth.of(taken.getYear(), taken.getMonth());
            Segment currentSegment = segments.computeIfAbsent(yearMonth, (yM) -> {
                Segment segment = new Segment();
                segment.setStartDate(yM.atDay(1));
                segment.setEndDate(yM.atEndOfMonth());
                return segment;
            });
            currentSegment.setCount(currentSegment.getCount() + 1);
        }
        segmentRepo.saveAll(segments.values());
    }
}
