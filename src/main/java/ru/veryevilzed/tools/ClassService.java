package ru.veryevilzed.tools;

/**
 * Единый интерфейс доступа к сервисам
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
