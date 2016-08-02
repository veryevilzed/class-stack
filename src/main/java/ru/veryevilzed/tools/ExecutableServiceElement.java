package ru.veryevilzed.tools;

import com.squareup.javapoet.MethodSpec;
import org.springframework.util.StringUtils;

import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;

/**
 * Created by zed on 02.08.16.
 */
public class ExecutableServiceElement {

    public ExecutableElement executableElement;
    public TypeElement typeElement;

    public SmartClassService serviceAnnotation;
    public SmartClassMethod methodAnnotation;


    public String fieldServiceName() {
        return StringUtils.uncapitalize(typeElement.getSimpleName().toString());
    }

}
