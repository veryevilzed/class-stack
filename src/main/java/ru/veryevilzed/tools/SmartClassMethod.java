package ru.veryevilzed.tools;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Метод сервиса указывает на точку входа в структуре класса
 * Created by zed on 02.08.16.
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.SOURCE)
public @interface SmartClassMethod {
    /**
     * Элемент класса != null
     * @return Значение
     */
    String value();

    /**
     * указывает на флаг bool
     * @return false
     */
    boolean isBoolean() default false;

    /**
     * Сравнивает аргумент
     * @return Сравнивает аргумент
     */
    String equals() default "";
}
