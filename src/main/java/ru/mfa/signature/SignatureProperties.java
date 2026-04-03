package ru.mfa.signature;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Getter;
import lombok.Setter;

@Component
@ConfigurationProperties(prefix = "signature")
@Getter
@Setter
public class SignatureProperties {

    /**
     * Путь к файлу keystore.
     * Поддерживаемые форматы:
     * - ресурс classpath (например: classpath:signing.jks)
     * - URI файла (например: file:/opt/app/keys/signing.jks)
     * - обычный путь файловой системы (например: /opt/app/keys/signing.jks)
     */
    private String keyStorePath;

    /**
     * Тип Java keystore.
     * По умолчанию используется JKS, если значение не задано.
     */
    private String keyStoreType = "JKS";

    /**
     * Пароль для открытия файла keystore.
     * Обязательное поле.
     */
    private String keyStorePassword;

    /**
     * Алиас записи ключа внутри keystore.
     * Обязательное поле.
     */
    private String keyAlias;

    /**
     * Пароль для записи приватного ключа.
     * Необязательное поле: если пусто, используется keyStorePassword.
     */
    private String keyPassword;

    private String algorithm = "SHA256withRSA";
}
