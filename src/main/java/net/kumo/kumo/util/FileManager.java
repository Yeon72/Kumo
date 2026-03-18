package net.kumo.kumo.util;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * 프로젝트 내부 서버 스토리지로의 물리적 파일 저장(Upload) 및 삭제 등
 * 로컬 파일 I/O 시스템 연산을 공통적으로 수행하는 유틸리티 컴포넌트입니다.
 */
@Slf4j
@RequiredArgsConstructor
@Component
public class FileManager {

    private final String UPLOAD_DIR = System.getProperty("user.dir") + "/src/main/resources/static/uploads/";

    /**
     * 클라이언트로부터 업로드된 원본 파일을 물리적 스토리지 폴더에 안전하게 저장하고,
     * 브라우저에서 렌더링 가능한 웹 가상 URL 경로를 반환합니다.
     * 이름 중복 방지를 위해 업로드 일자와 UUID를 결합하여 파일명을 재생성합니다.
     *
     * @param file 업로드할 다중 파트 파일 객체
     * @return 데이터베이스 저장용 웹 접근 URL (예: /uploads/20260208_uuid.png)
     * @throws RuntimeException I/O 스트림 예외 발생 시 처리
     */
    public String saveFile(MultipartFile file) {
        if (file.isEmpty()) {
            return null;
        }

        try {
            File directory = new File(UPLOAD_DIR);
            if (!directory.exists()) {
                directory.mkdirs();
            }

            String originalFileName = file.getOriginalFilename();
            String extension = originalFileName.substring(originalFileName.lastIndexOf("."));
            String dateString = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyyMMdd"));
            String uuidString = UUID.randomUUID().toString();

            String savedFileName = dateString + "_" + uuidString + extension;

            File destFile = new File(UPLOAD_DIR + savedFileName);
            file.transferTo(destFile);

            log.info("파일 스토리지 저장 완료: {}", destFile.getAbsolutePath());

            return "/uploads/" + savedFileName;

        } catch (IOException e) {
            log.error("파일 스토리지 저장 실패", e);
            throw new RuntimeException("파일 저장 중 시스템 I/O 오류가 발생했습니다.");
        }
    }

    /**
     * 웹 접근 경로(URL)를 기반으로 해당 파일의 물리적 스토리지 저장소를 역추적하여 삭제합니다.
     *
     * @param fileName 데이터베이스에 저장되어 있는 웹 접근 파일명 또는 경로 URL
     * @return 삭제 성공 여부 (존재하지 않거나 실패 시 false 반환)
     */
    public boolean deleteFile(String fileName) {
        try {
            String actualFileName = fileName.replace("/uploads/", "");
            Path filePath = Paths.get(UPLOAD_DIR, actualFileName);
            return Files.deleteIfExists(filePath);
        } catch (IOException e) {
            log.error("파일 스토리지 삭제 실패", e);
            return false;
        }
    }
}