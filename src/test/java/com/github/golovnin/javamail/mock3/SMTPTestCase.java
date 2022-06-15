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

import java.util.Properties;
import jakarta.mail.Address;
import jakarta.mail.Folder;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import org.junit.FixMethodOrder;
import org.junit.Test;
import org.junit.runners.MethodSorters;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class SMTPTestCase extends AbstractTestCase {

    @Override
    protected Properties getProperties() {
        final Properties props = super.getProperties();
        props.setProperty("mail.transport.protocol.rfc822", "mock_smtp");
        return props;
    }

    @Test(expected = MockTestException.class)
    public void test1SendMessageFailure() throws Exception {
        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        mb.getInbox().setSimulateError(true);

        final MimeMessage msg = new MimeMessage(session);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        try {
            Transport.send(msg);
        } catch (final com.sun.mail.util.MailConnectException e) {
            throw e;
        } catch (final MessagingException e) {
            throw new MockTestException(e);
        }
    }

    @Test
    public void test2SendMessage2() throws Exception {
        final Transport transport = session.getTransport(
            Providers.getSMTPProvider("makes_no_difference_here", true, true));

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test 1");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        transport.sendMessage(msg, new Address[] { new InternetAddress("hendrik@unknown.com") });

        final Store store = session.getStore("mock_pop3");
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        assertEquals(1, inbox.getMessageCount());
        assertNotNull(inbox.getMessage(1));
        assertEquals("Test 1", inbox.getMessage(1).getSubject());
        inbox.close(false);
    }

    @Test
    public void test3SendMessage() throws Exception {
        Session.getDefaultInstance(getProperties());

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test 1");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        Transport.send(msg);

        final Store store = session.getStore("mock_pop3");
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        assertEquals(1, inbox.getMessageCount());
        assertNotNull(inbox.getMessage(1));
        assertEquals("Test 1", inbox.getMessage(1).getSubject());
        inbox.close(false);
    }

}
