package com.example.Sla_Reuniao.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Service
public class EmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailService.class);

    private final JavaMailSender mailSender;

    public EmailService(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public void enviarEmail(String para, String assunto, String texto) {
        SimpleMailMessage mensagem = new SimpleMailMessage();
        mensagem.setTo(para);
        mensagem.setSubject(assunto);
        mensagem.setText(texto);
        mailSender.send(mensagem);
    }

    public boolean enviarEmailSilencioso(String para, String assunto, String texto) {
        if (para == null || para.isBlank()) {
            log.warn("E-mail ignorado: destinatário vazio. Assunto: {}", assunto);
            return false;
        }
        try {
            enviarEmail(para, assunto, texto);
            return true;
        } catch (Exception e) {
            log.error("Falha ao enviar e-mail para {}", para, e);
            return false;
        }
    }

    public boolean notifyResponsavelAtribuido(String toEmail, Long tarefaId, String titulo) {
        return enviarNotificacao(
                toEmail,
                "Tarefa #" + tarefaId + " · responsável atribuído",
                "Você foi definido como responsável.\nTarefa #" + tarefaId + ": " + tituloSeguro(titulo)
        );
    }

    public boolean notifyNovoComentario(String toEmail, Long tarefaId, String titulo) {
        return enviarNotificacao(
                toEmail,
                "Tarefa #" + tarefaId + " · novo comentário",
                "Um novo comentário foi registrado.\nTarefa #" + tarefaId + ": " + tituloSeguro(titulo)
        );
    }

    private boolean enviarNotificacao(String toEmail, String assunto, String texto) {
        return enviarEmailSilencioso(toEmail, assunto, texto);
    }

    private static String tituloSeguro(String titulo) {
        return titulo == null || titulo.isBlank() ? "(sem título)" : titulo;
    }
}
