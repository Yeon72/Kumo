package net.kumo.kumo.service;

import lombok.RequiredArgsConstructor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Random;

/**
 * 이메일 발송과 관련된 비즈니스 로직을 처리하는 서비스 클래스입니다.
 * 회원가입 인증, 비밀번호 찾기 등에 사용되는 인증번호 메일 발송 기능을 제공합니다.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmailService {

    private final JavaMailSender javaMailSender;

    /**
     * 수신자 이메일, 제목, 본문 내용을 받아 실제 이메일을 발송합니다.
     *
     * @param toEmail 수신자 이메일 주소
     * @param title   이메일 제목
     * @param text    이메일 본문 내용
     * @throws RuntimeException 메일 발송 서버 통신 실패 시 발생
     */
    public void sendEmail(String toEmail, String title, String text) {
        SimpleMailMessage emailForm = createEmailForm(toEmail, title, text);

        try {
            javaMailSender.send(emailForm);
        } catch (RuntimeException e) {
            e.printStackTrace();
            throw new RuntimeException("이메일 발송에 실패했습니다: " + e.getMessage());
        }
    }

    /**
     * 메일 발송에 필요한 SimpleMailMessage 객체를 생성하여 반환합니다.
     *
     * @param toEmail 수신자 이메일 주소
     * @param title   이메일 제목
     * @param text    이메일 본문 내용
     * @return 조립이 완료된 SimpleMailMessage 객체
     */
    private SimpleMailMessage createEmailForm(String toEmail, String title, String text) {
        SimpleMailMessage message = new SimpleMailMessage();
        message.setTo(toEmail);
        message.setSubject(title);
        message.setText(text);

        return message;
    }

    /**
     * 6자리의 난수 인증번호를 생성하고, 다국어(한국어/일본어) 템플릿이 적용된
     * 인증 메일을 대상자에게 발송합니다.
     *
     * @param email 인증 번호를 받을 수신자 이메일 주소
     * @return 생성 및 발송된 6자리 인증 번호 문자열
     */
    public String sendCertigicationMail(String email) {
        String certificationNumber = createKey();
        String title = "[KUMO] 認証番号のお知らせ / 인증번호 안내";
        StringBuilder content = new StringBuilder();

        content.append("KUMOをご利用いただきありがとうございます。\n");
        content.append("下記の認証番号を入力してください。\n\n");
        content.append("認証番号: ").append(certificationNumber).append("\n\n");
        content.append("※ 他人に知られないようご注意ください。\n");

        content.append("\n--------------------------------------------------\n\n");

        content.append("KUMO를 이용해 주셔서 감사합니다.\n");
        content.append("아래 인증번호를 입력해 주세요.\n\n");
        content.append("인증번호: ").append(certificationNumber).append("\n\n");
        content.append("※ 타인에게 노출되지 않도록 주의해 주세요.");

        sendEmail(email, title, content.toString());

        return certificationNumber;
    }

    /**
     * 0부터 9까지의 숫자로 이루어진 6자리 무작위 인증 번호(Key)를 생성합니다.
     *
     * @return 6자리 난수 문자열
     */
    public String createKey() {
        Random random = new Random();
        StringBuilder key = new StringBuilder();
        for (int i = 0; i < 6; i++) {
            key.append(random.nextInt(10));
        }

        return key.toString();
    }
}