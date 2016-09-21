package ru.linachan.email;

import ru.linachan.yggdrasil.YggdrasilCore;

import javax.mail.Message;

public interface EMailHandler {

    YggdrasilCore core = YggdrasilCore.INSTANCE;

    void handle(String title, Message message);
}
