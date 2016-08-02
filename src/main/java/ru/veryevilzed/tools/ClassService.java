package ru.veryevilzed.tools;

/**
 * Created by zed on 02.08.16.
 */
public interface ClassService {

    /**
     * Execute
     * @param incoming входящий класс
     * @param context контекст
     * @return контекст
     */
    Object execute(Object incoming, Object context);

}
