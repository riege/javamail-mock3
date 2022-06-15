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

import java.util.Arrays;
import java.util.Properties;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Folder;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import com.sun.mail.pop3.POP3Folder;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

public class POP3TestCase extends AbstractTestCase {

    @Override
    protected Properties getProperties() {
        final Properties props = super.getProperties();
        props.setProperty("mail.store.protocol", "mock_pop3s");
        return props;
    }

    @Test
    public void testAddMessages() throws Exception {
        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        mf.add(msg); // 11
        mf.add(msg); // 12
        mf.add(msg); // 13

        final Store store = session.getStore();
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_ONLY);
        assertEquals(3, inbox.getMessageCount());
        assertNotNull(inbox.getMessage(1));

        inbox.close(true);

        assertEquals(3, inbox.getMessageCount());

        inbox.open(Folder.READ_ONLY);
        inbox.getMessage(1).setFlag(Flag.DELETED, true);

        inbox.close(true);
        inbox.open(Folder.READ_ONLY);
        assertEquals(2, inbox.getMessageCount());
        assertTrue(inbox instanceof POP3Folder);
        assertEquals("12", ((POP3Folder) inbox).getUID(inbox.getMessage(1)));
        inbox.close(true);
    }

    @Test
    public void testDefaultFolder() throws Exception {
        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        mf.add(msg); // 11
        mf.add(msg); // 12
        mf.add(msg); // 13
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore("mock_pop3");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        assertEquals("[INBOX]", Arrays.toString(defaultFolder.list()));

        assertEquals(3, inbox.getMessageCount());
        assertNotNull(inbox.getMessage(1));

        inbox.close(true);

        assertEquals(3, inbox.getMessageCount());

        inbox.open(Folder.READ_ONLY);
        inbox.getMessage(1).setFlag(Flag.DELETED, true);

        inbox.close(true);
        inbox.open(Folder.READ_WRITE);
        assertEquals(2, inbox.getMessageCount());
        assertTrue(inbox instanceof POP3Folder);
        assertEquals("12", ((POP3Folder) inbox).getUID(inbox.getMessage(1)));
        inbox.close(true);
    }

    @Test(expected = MockTestException.class)
    public void testOnlyInbox() throws Exception {
        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        mf.add(msg); // 11
        mf.add(msg); // 12
        mf.add(msg); // 13
        mb.getRoot().getOrAddSubFolder("test").create().add(msg);

        final Store store = session.getStore(Providers.getPOP3Provider("makes_no_differernce", false, true));
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();

        try {
            defaultFolder.getFolder("test");
        } catch (final MessagingException e) {
            throw new MockTestException(e);
        }
    }

}
