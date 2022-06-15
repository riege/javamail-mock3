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
import jakarta.mail.Folder;
import jakarta.mail.Message;
import jakarta.mail.Message.RecipientType;
import jakarta.mail.Session;
import jakarta.mail.Store;
import jakarta.mail.event.MessageCountEvent;
import jakarta.mail.event.MessageCountListener;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import com.sun.mail.imap.IMAPFolder;

public class Main {

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
                    Thread.sleep(500);
                } catch (final Exception e) {
                    exception = e;
                }
            }

            // System.out.println("leave run()");
        }
    }

    protected static Properties getProperties() {

        final Properties props = new Properties();
        props.setProperty("mail.store.protocol", "mock_imaps");
        return props;
    }

    public static void main(final String[] args) throws Exception {

        final Session session = Session.getInstance(getProperties());

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

        final IdleThread it2 = new IdleThread(test);
        it2.start();

        test.addMessages(new Message[] { msg });
        test.addMessages(new Message[] { msg });
        test.addMessages(new Message[] { msg });

        // test.close(true);

        System.out.println(it.getException());
        System.out.println(mcl.getAddedCount());
        System.out.println(mcl.getRemovedCount());
        System.out.println(test.getMessageCount());
        System.out.println(it.getIdleCount());

        // System.exit(1);

    }

}
