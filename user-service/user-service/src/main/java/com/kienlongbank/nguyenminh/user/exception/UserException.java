package com.kienlongbank.nguyenminh.user.exception;

import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;

import java.util.Locale;

public class UserException extends RuntimeException {
    private final String messageKey;
    private final Object[] messageArgs;
    private MessageSource messageSource;

    public UserException(String message) {
        super(message);
        this.messageKey = null;
        this.messageArgs = null;
    }
    
    /**
     * Tạo exception với message key từ message resource
     * 
     * @param messageKey key của message trong resource
     */
    public UserException(String messageKey, Object[] messageArgs) {
        super(messageKey);  // Lưu messageKey làm default message
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
    }
    
    /**
     * Tạo exception với message key và MessageSource
     * 
     * @param messageKey key của message trong resource
     * @param messageSource source để lấy message
     */
    public UserException(String messageKey, Object[] messageArgs, MessageSource messageSource) {
        super(messageKey);  // Lưu messageKey làm default message
        this.messageKey = messageKey;
        this.messageArgs = messageArgs;
        this.messageSource = messageSource;
    }

    /**
     * Lấy message đã localize nếu có messageSource và messageKey
     * Nếu không có sẽ trả về message gốc
     */
    @Override
    public String getMessage() {
        if (messageSource != null && messageKey != null) {
            Locale locale = LocaleContextHolder.getLocale();
            return messageSource.getMessage(messageKey, messageArgs, super.getMessage(), locale);
        }
        return super.getMessage();
    }
    
    /**
     * Thiết lập MessageSource sau khi exception đã được tạo
     */
    public void setMessageSource(MessageSource messageSource) {
        this.messageSource = messageSource;
    }
    
    /**
     * Lấy message key dùng để i18n
     */
    public String getMessageKey() {
        return messageKey;
    }
    
    /**
     * Lấy các tham số cho message
     */
    public Object[] getMessageArgs() {
        return messageArgs;
    }
} 