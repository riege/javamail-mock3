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

import jakarta.mail.Folder;
import jakarta.mail.MessagingException;

import com.sun.mail.imap.DefaultFolder;

import static com.github.golovnin.javamail.mock3.MockMailbox.isInbox;

public final class IMAPDefaultMockFolder extends DefaultFolder {

    private static final Folder[] EMPTY = new Folder[0];

    private final MockMailbox mailbox;
    private final IMAPMockStore store;

    IMAPDefaultMockFolder(IMAPMockStore store, MockMailbox mailbox) {
        super(store);
        this.mailbox = mailbox;
        this.store = store;
    }

    @Override
    public boolean create(int type) throws MessagingException {
        return true;
    }

    @Override
    public boolean exists() throws MessagingException {
        return true;
    }

    @Override
    public Folder getFolder(String name) throws MessagingException {
        if (isInbox(name)) {
            return new IMAPMockFolder(store, mailbox.getInbox());
        }

        return new IMAPMockFolder(store, mailbox.getRoot().getOrAddSubFolder(name));
    }

    @Override
    public int getType() throws MessagingException {
        checkExists();
        return HOLDS_FOLDERS;
    }

    @Override
    public synchronized Folder[] list(String pattern) throws MessagingException {
        return mailbox.getRoot().getChildren()
            .stream()
            .filter(MailboxFolder::isExists)
            .map(mf -> new IMAPMockFolder(store, mf))
            .toArray(Folder[]::new);
    }

    @Override
    public Folder[] listSubscribed(String pattern) throws MessagingException {
        return EMPTY;
    }

}
