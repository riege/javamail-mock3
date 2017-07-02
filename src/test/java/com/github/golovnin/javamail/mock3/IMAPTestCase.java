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
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.Message;
import javax.mail.Message.RecipientType;
import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.Store;
import javax.mail.UIDFolder;
import javax.mail.event.MessageCountEvent;
import javax.mail.event.MessageCountListener;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.IMAPStore;
import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

public class IMAPTestCase extends AbstractTestCase {

    private static class IdleMessageCountListener implements MessageCountListener {

        private int addedCount;
        private int removedCount;

        protected int getAddedCount() {
            return addedCount;
        }

        protected int getRemovedCount() {
            return removedCount;
        }

        @Override
        public void messagesAdded(final MessageCountEvent e) {
            addedCount++;

        }

        @Override
        public void messagesRemoved(final MessageCountEvent e) {
            removedCount++;

        }

    }

    private static class IdleThread extends Thread {
        private Exception exception;
        private final Folder folder;
        private int idleCount;

        public IdleThread(final Folder folder) {
            super();
            this.folder = folder;
        }

        protected Exception getException() {
            return exception;
        }

        protected int getIdleCount() {
            return idleCount;
        }

        @Override
        public void run() {

            while (!Thread.interrupted()) {
                try {
                    // System.out.println("enter idle");
                    ((IMAPFolder) folder).idle();
                    idleCount++;
                    // System.out.println("leave idle");
                } catch (final Exception e) {
                    exception = e;
                }
            }

            // System.out.println("leave run()");
        }
    }

    @Override
    protected Properties getProperties() {
        final Properties props = super.getProperties();
        props.setProperty("mail.store.protocol", "mock_imaps");
        return props;
    }

    @Test(expected = MockTestException.class)
    public void testACLUnsupported() throws Exception {
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

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder test = defaultFolder.getFolder("test");

        final IMAPFolder testImap = (IMAPFolder) test;

        try {
            testImap.getACL();
        } catch (final MessagingException e) {
            throw new MockTestException(e);
        }
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
        inbox.open(Folder.READ_WRITE);
        assertEquals(3, inbox.getMessageCount());
        assertNotNull(inbox.getMessage(1));

        inbox.close(true);

        assertEquals(3, inbox.getMessageCount());

        inbox.open(Folder.READ_WRITE);
        inbox.getMessage(1).setFlag(Flag.DELETED, true);

        inbox.close(true);

        assertEquals(2, inbox.getMessageCount());
        assertTrue(inbox instanceof UIDFolder);
        inbox.open(Folder.READ_WRITE);
        assertEquals(12L, ((UIDFolder) inbox).getUID(inbox.getMessage(1)));
        inbox.close(true);
    }

    @Test
    // (expected = MockTestException.class)
    public void testAppendFailMessage() throws Exception {
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

        final Store store = session.getStore();
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_ONLY);

        try {
            inbox.appendMessages(new MimeMessage[] { msg });
        } catch (final IllegalStateException e) {
            // throw new MockTestException(e);
        }

        // Assert.fail("Exception expected before this point");

        assertEquals(4, inbox.getMessageCount());

        inbox.close(false);
    }

    @Test
    public void testAppendMessage() throws Exception {
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

        final Store store = session.getStore(Providers.getIMAPProvider("makes_no_difference_here", true, true));
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        inbox.appendMessages(new MimeMessage[] { msg });

        assertEquals(4, inbox.getMessageCount());

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

        final Store store = session.getStore("mock_imaps");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        assertEquals("[INBOX, test]", Arrays.toString(defaultFolder.list()));

        assertEquals(3, inbox.getMessageCount());
        assertNotNull(inbox.getMessage(1));

        inbox.close(true);

        assertEquals(3, inbox.getMessageCount());

        inbox.open(Folder.READ_WRITE);
        inbox.getMessage(1).setFlag(Flag.DELETED, true);

        inbox.close(true);
        inbox.open(Folder.READ_WRITE);
        assertEquals(2, inbox.getMessageCount());
        assertTrue(inbox instanceof UIDFolder);
        assertEquals(12L, ((UIDFolder) inbox).getUID(inbox.getMessage(1)));
        inbox.close(true);
    }

    @Test
    public void testIDLESupported() throws Exception {
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

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final IMAPFolder test = (IMAPFolder) defaultFolder.getFolder("test");

        final IdleMessageCountListener mcl = new IdleMessageCountListener();
        test.addMessageCountListener(mcl);

        test.open(Folder.READ_WRITE);

        final IdleThread it = new IdleThread(test);
        it.start();

        test.addMessages(new Message[] { msg });
        test.addMessages(new Message[] { msg });
        test.addMessages(new Message[] { msg });

        Thread.sleep(500);

        it.interrupt();
        it.join();

        test.close(true);

        assertNull(it.getException());
        assertEquals(3, mcl.getAddedCount());
        assertEquals(0, mcl.getRemovedCount());
        assertEquals(4, test.getMessageCount());

    }

    @Test
    public void testNotOnlyInbox() throws Exception {
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

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder test = defaultFolder.getFolder("test");

        assertNotNull(test);

        final Folder inbox = defaultFolder.getFolder("INBOX");

        assertNotNull(inbox);
    }

    @Test(expected = MockTestException.class)
    public void testQUOTAUnsupported() throws Exception {
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

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder test = defaultFolder.getFolder("test");

        final IMAPStore imapStore = (IMAPStore) store;

        try {
            imapStore.getQuota("");
        } catch (final MessagingException e) {
            throw new MockTestException(e);
        }
    }

    @Test
    public void testRenameWithSubfolder() throws Exception {
        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder root = store.getDefaultFolder();
        final Folder level1 = root.getFolder("LEVEL1");
        level1.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        final Folder level2 = level1.getFolder("LEVEL2");
        level2.create(Folder.HOLDS_FOLDERS | Folder.HOLDS_MESSAGES);
        level1.appendMessages(new Message[] { msg, msg });
        level2.appendMessages(new Message[] { msg });

        assertTrue(level1.exists());
        assertEquals("LEVEL1", level1.getFullName());
        assertEquals("LEVEL1", level1.getName());
        assertEquals(2, level1.getMessageCount());

        assertTrue(level2.exists());
        assertEquals("LEVEL1/LEVEL2", level2.getFullName());
        assertEquals("LEVEL2", level2.getName());
        assertEquals(1, level2.getMessageCount());
        assertEquals(2, root.list().length);

        // getFolder creates a store
        level1.renameTo(store.getFolder("LEVEL-1R"));

        // TODO really need a create?
        assertTrue(!store.getFolder("LEVEL1").exists());

        assertTrue(level1.exists());
        assertEquals("LEVEL-1R", level1.getFullName());
        assertEquals("LEVEL-1R", level1.getName());
        assertEquals(2, level1.getMessageCount());

        assertTrue(level2.exists());
        assertEquals("LEVEL-1R/LEVEL2", level2.getFullName());
        assertEquals("LEVEL2", level2.getName());
        assertEquals(1, level2.getMessageCount());

        assertEquals(2, root.list().length);
    }

    @Test
    public void testGetMessageByUnknownUID() throws Exception {
        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder inbox = store.getFolder("INBOX");
        inbox.open(Folder.READ_WRITE);
        final IMAPFolder imapInbox = (IMAPFolder) inbox;
        assertNull(imapInbox.getMessageByUID(666));
    }

    @Test
    public void testGetMessages() throws Exception {
        final MockMailbox mb = MockMailbox.get("hendrik@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("hendrik@unknown.com"));
        mf.add(msg);

        final Store store = session.getStore("mock_imap");
        store.connect("hendrik@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final Folder inbox = defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        assertEquals(1, inbox.getMessageCount());
        assertEquals(1, inbox.getMessages().length);
        inbox.close(true);
    }

    @Test
    public void testMoveMessages() throws Exception {
        final MockMailbox mb = MockMailbox.get("andrej@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("andrej@unknown.com"));
        mf.add(msg);

        final Store store = session.getStore("mock_imap");
        store.connect("andrej@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final IMAPFolder inbox = (IMAPFolder) defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        assertEquals(1, inbox.getMessageCount());
        assertEquals(1, inbox.getMessages().length);

        IMAPFolder folder = (IMAPFolder) store.getFolder("Archive");
        if (!folder.exists()) {
          folder.create(Folder.HOLDS_MESSAGES);
        }
        folder.open(Folder.READ_WRITE);

        Message[] messages = inbox.getMessages();
        inbox.moveMessages(messages, folder);

        assertEquals(0, inbox.getMessageCount());
        assertEquals(0, inbox.getMessages().length);

        assertEquals(1, folder.getMessageCount());
        assertEquals(1, folder.getMessages().length);

        inbox.close(true);
        folder.close(true);
    }

    @Test
    public void testMoveUIDMessages() throws Exception {
        final MockMailbox mb = MockMailbox.get("andrej@unknown.com");
        final MailboxFolder mf = mb.getInbox();

        final MimeMessage msg = new MimeMessage((Session) null);
        msg.setSubject("Test");
        msg.setFrom("from@sender.com");
        msg.setText("Some text here ...");
        msg.setRecipient(RecipientType.TO, new InternetAddress("andrej@unknown.com"));
        mf.add(msg);

        final Store store = session.getStore("mock_imap");
        store.connect("andrej@unknown.com", null);
        final Folder defaultFolder = store.getDefaultFolder();
        final IMAPFolder inbox = (IMAPFolder) defaultFolder.getFolder("INBOX");

        inbox.open(Folder.READ_WRITE);

        assertEquals(1, inbox.getMessageCount());
        assertEquals(1, inbox.getMessages().length);

        IMAPFolder folder = (IMAPFolder) store.getFolder("Archive");
        if (!folder.exists()) {
          folder.create(Folder.HOLDS_MESSAGES);
        }
        folder.open(Folder.READ_WRITE);

        Message[] messages = inbox.getMessages();
        AppendUID[] result = inbox.moveUIDMessages(messages, folder);

        assertEquals(0, inbox.getMessageCount());
        assertEquals(0, inbox.getMessages().length);

        assertEquals(1, folder.getMessageCount());
        assertEquals(1, folder.getMessages().length);

        assertEquals(1, result.length);
        assertEquals(folder.getUIDValidity(), result[0].uidvalidity);

        inbox.close(true);
        folder.close(true);
    }

}
