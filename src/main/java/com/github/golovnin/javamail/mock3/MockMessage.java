/*
 *  JavaMail Mock3 Provider - open source mock classes for mock up JavaMail
 *  =======================================================================
 *
 *  Copyright (C) 2014 by Hendrik Saly (http://saly.de)
 *
 *  Based on ideas from Kohsuke Kawaguchi's Mock-javamail
 *  (https://java.net/projects/mock-javamail)
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not
 *  use this file except in compliance with
 *  the License. You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on
 *  an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 *  either express or implied. See the License for the
 *  specific language governing permissions and limitations under the License.
 */
package com.github.golovnin.javamail.mock3;

import java.util.Date;

import javax.activation.DataHandler;
import javax.mail.Address;
import javax.mail.Flags;
import javax.mail.Folder;
import javax.mail.IllegalWriteException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.internet.MimeMessage;

public final class MockMessage extends MimeMessage implements
    Comparable<MockMessage>
{

    public interface FlagChangeListener {
        void onFlagChange(MockMessage msg, Flags flags, boolean set);
    }

    private final MailboxFolder mbf;
    private final long mockid;
    private final FlagChangeListener flagChangeListener;
    private final Folder folder;

    MockMessage(Message source, Folder folder) throws MessagingException {
        super((MimeMessage) source);
        this.mockid = ((MockMessage) source).mockid;
        this.flagChangeListener = ((MockMessage) source).flagChangeListener;
        this.mbf = ((MockMessage) source).mbf;
        this.folder = folder;
        setMessageNumber(source.getMessageNumber());
    }

    MockMessage(MimeMessage source, long mockid, MailboxFolder mbf,
        FlagChangeListener flagChangeListener) throws MessagingException
    {
        super(source);
        this.mockid = mockid;
        this.flagChangeListener = flagChangeListener;
        this.mbf = mbf;
        this.folder = null;
    }

    @Override
    public void addFrom(Address[] addresses) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void addHeader(String name, String value) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void addHeaderLine(String line) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void addRecipient(javax.mail.Message.RecipientType type,
        Address address) throws MessagingException
    {
        assertReadOnlyMessage();
    }

    @Override
    public void addRecipients(javax.mail.Message.RecipientType type,
        Address[] addresses) throws MessagingException
    {
        assertReadOnlyMessage();
    }

    @Override
    public void addRecipients(javax.mail.Message.RecipientType type,
        String addresses) throws MessagingException
    {
        assertReadOnlyMessage();
    }

    @Override
    public int compareTo(MockMessage o) {
        return Long.compare(this.mockid, o.mockid);
    }

    @Override
    public synchronized Folder getFolder() {
        if (folder == null) {
            throw new RuntimeException("wrong/unshaded mock message");
        } else {
            return folder;
        }
    }

    public long getMockid() {
        return mockid;
    }

    @Override
    public void removeHeader(String name) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void saveChanges() throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setContent(Multipart mp) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setContent(Object o, String type) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setContentID(String cid) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setContentLanguage(String[] languages) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setContentMD5(String md5) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setDataHandler(DataHandler content) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setDescription(String description) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setDescription(String description, String charset)
        throws MessagingException
    {
        assertReadOnlyMessage();
    }

    @Override
    public void setDisposition(String disposition) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    protected void setExpunged(boolean expunged) {
        super.setExpunged(expunged);
    }

    @Override
    public void setFileName(String filename) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public synchronized void setFlags(Flags flag, boolean set)
        throws MessagingException
    {
        super.setFlags(flag, set);

        if (flagChangeListener != null) {
            flagChangeListener.onFlagChange(this, flag, set);
        }
    }

    @Override
    public void setFrom() throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setFrom(Address address) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setFrom(String address) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setHeader(String name, String value) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setRecipient(javax.mail.Message.RecipientType type,
        Address address) throws MessagingException
    {
        assertReadOnlyMessage();
    }

    @Override
    public void setRecipients(javax.mail.Message.RecipientType type,
        Address[] addresses) throws MessagingException
    {
        assertReadOnlyMessage();
    }

    @Override
    public void setRecipients(javax.mail.Message.RecipientType type,
        String addresses) throws MessagingException
    {
        assertReadOnlyMessage();
    }

    @Override
    public void setReplyTo(Address[] addresses) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setSender(Address address) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setSentDate(Date d) throws MessagingException {
        assertReadOnlyMessage();
    }

    void setSpecialHeader(String name, String value) throws MessagingException {
        super.addHeader(name, value);
    }

    @Override
    public void setSubject(String subject) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setSubject(String subject, String charset) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setText(String text) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setText(String text, String charset) throws MessagingException {
        assertReadOnlyMessage();
    }

    @Override
    public void setText(String text, String charset, String subtype)
        throws MessagingException
    {
        assertReadOnlyMessage();
    }

    @Override
    protected void setMessageNumber(int msgnum) {
        super.setMessageNumber(msgnum);
    }

    private static void assertReadOnlyMessage() throws IllegalWriteException {
        throw new IllegalWriteException("Mock messages are read-only");
    }

}
