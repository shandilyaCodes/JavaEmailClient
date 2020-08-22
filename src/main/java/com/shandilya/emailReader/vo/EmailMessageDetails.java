package com.shandilya.emailReader.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.mail.Folder;
import javax.mail.Message;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class EmailMessageDetails {

    private Message[] messages;
    private Folder inbox;
}