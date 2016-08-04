package ru.veryevilzed.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Помечает класс для регистрации в сервисе class-stack
 * Created by zed on 02.08.16.
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.SOURCE)
public @interface SmartClassService {

    /**
     * Имя класса вместе с пакетом
     * @return Имя класса вместе с пакетом
     */
    String incoming();

    /**
     * Контекстный класс
     * @return Контекстный класс (default java.lang.Object)
     */
    String context() default "java.lang.Object";

}
