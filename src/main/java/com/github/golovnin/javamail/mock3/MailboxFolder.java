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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;
import jakarta.mail.Flags;
import jakarta.mail.Flags.Flag;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.UIDFolder;
import jakarta.mail.internet.MimeMessage;
import jakarta.mail.search.SearchTerm;

public class MailboxFolder implements MockMessage.FlagChangeListener {

    private static final Flags RECENT_FLAGS = new Flags(Flag.RECENT);
    private static final Flags DELETED_FLAGS = new Flags(Flag.DELETED);

    public interface MailboxEventListener {

        void folderCreated(MailboxFolder mf);

        void folderDeleted(MailboxFolder mf);

        void folderRenamed(String from, MailboxFolder to);

        void messageAdded(MailboxFolder mf, MockMessage msg);

        void messageChanged(MailboxFolder mf, MockMessage msg,
            boolean headerChanged, boolean flagsChanged);

        void messageExpunged(MailboxFolder mf, MockMessage msg, boolean removed);

        void uidInvalidated();

    }

    static final char SEPARATOR = '/';

    private final MockMailbox mailbox;
    private final List<MailboxFolder> children = new ArrayList<>();
    private final Map<Long, MockMessage> messages = new TreeMap<>();
    private final List<MailboxEventListener> mailboxEventListeners =
        Collections.synchronizedList(new ArrayList<>());

    private boolean exists = true;

    private String name;
    private MailboxFolder parent;
    private boolean simulateError = false;
    private boolean subscribed;

    private long uidValidity = 50;
    private long uniqueMessageId = 10;

    protected MailboxFolder(String name, MockMailbox mb, boolean exists) {
        if (name == null) {
            this.name = "";
        } else {
            this.name = name;
        }

        this.mailbox = mb;
        this.exists = exists;
    }

    public synchronized MockMessage add(MimeMessage e) throws MessagingException {
        checkExists();

        uniqueMessageId++;

        MockMessage mockMessage = new MockMessage(e, uniqueMessageId, this, this);

        mockMessage.setSpecialHeader("Message-ID", String.valueOf(uniqueMessageId));
        mockMessage.setSpecialHeader("X-Mock-Folder", getFullName());
        mockMessage.setFlags(RECENT_FLAGS, true);

        messages.put(uniqueMessageId, mockMessage);

        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
            mailboxEventListener.messageAdded(this, mockMessage);
        }

        return mockMessage;
    }

    public synchronized void addMailboxEventListener(MailboxEventListener l) {
        if (l != null) {
            mailboxEventListeners.add(l);
        }
    }

    protected MailboxFolder addSpecialSubFolder(String name) {
        MailboxFolder mbt = new MailboxFolder(name, mailbox, true);
        mbt.parent = this;
        children.add(mbt);
        return mbt;
    }

    private void checkExists() {
        if (!exists) {
            throw new IllegalStateException("folder does not exist");
        }
    }

    private static void checkFolderName(String name) {
        if (   name == null
            || name.trim().isEmpty()
            || MockMailbox.isInbox(name)
            || name.indexOf(SEPARATOR) > -1)
        {
            throw new IllegalArgumentException("name '" + name + "' is not valid");
        }
    }

    public synchronized MailboxFolder create() {
        if (exists) {
            throw new IllegalStateException("already exists");
        }
        checkFolderName(this.name);

        exists = true;

        // TODO set parent and/or children to exists?

        if (parent != null && !parent.exists) {
            parent.create();
        }

        /*children.clear();

        if (parent != null) {
            parent.children.add(this);
        }

        if (mailboxEventListener != null) {
            mailboxEventListener.folderCreated(this);
        }*/

        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
            mailboxEventListener.folderCreated(this);
        }

        return this;
    }

    public synchronized void deleteFolder(boolean recurse) {
        checkExists();
        checkFolderName(this.name);

        if (isRoot()) {
            throw new IllegalArgumentException("root cannot be deleted");
        }

        messages.clear();

        if (recurse) {
            for (MailboxFolder mf : getChildren()) {
                mf.deleteFolder(recurse);
            }
        }

        parent.children.remove(this);
        this.exists = false;

        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
            mailboxEventListener.folderDeleted(this);
        }
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof MailboxFolder) {
            MailboxFolder other = (MailboxFolder) obj;
            return Objects.equals(name, other.name)
                && Objects.equals(parent, other.parent);
        }
        return false;
    }

    public synchronized Collection<Message> expunge() throws MessagingException {
        checkExists();
        List<Message> expunged = new ArrayList<>();
        for (Message msg : getByFlags(DELETED_FLAGS, true)) {
            MockMessage message = (MockMessage) msg;
            expunged.add(messages.remove(message.getMockid()));
            message.setExpunged(true);

            for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
                mailboxEventListener.messageExpunged(this, message, true);
            }
        }

        return expunged;
    }

    public synchronized Collection<Message> expunge(Message[] msgs) throws MessagingException {
        checkExists();

        List<Long> toExpunge = new ArrayList<>();
        for (Message msg : msgs) {
            toExpunge.add(((MockMessage) msg).getMockid());
        }

        List<Message> expunged = new ArrayList<>();
        for (Message msg : getByFlags(DELETED_FLAGS, true)) {
            MockMessage message = (MockMessage) msg;
            if (!toExpunge.contains(message.getMockid())) {
                continue;
            }

            expunged.add(messages.remove(message.getMockid()));
            message.setExpunged(true);

            for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
                mailboxEventListener.messageExpunged(this, message, true);
            }
        }

        return expunged;
    }

    public synchronized void delete(Message[] msgs) throws MessagingException {
        checkExists();

        for (Message msg : msgs) {
            MockMessage message = (MockMessage) msg;

            if (messages.remove(message.getMockid()) != null) {
                message.setExpunged(true);

                for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
                    mailboxEventListener.messageExpunged(this, message, true);
                }
            }
        }
    }

    public synchronized Collection<Message> getByFlags(Flags flags,
        boolean mustSet) throws MessagingException
    {
        checkExists();
        List<Message> result = new ArrayList<>();
        int num = 0;

        for (MockMessage mockMessage : messages.values()) {
            if (   mustSet && mockMessage.getFlags().contains(flags)
                || !mustSet && !mockMessage.getFlags().contains(flags))
            {
                mockMessage.setMessageNumber(++num);
                result.add(mockMessage);
            }

        }
        return result;
    }

    public synchronized Message getById(long id) {
        checkExists();
        return messages.get(id);
    }

    public synchronized Collection<Message> getByIds(long start, long end) {
        checkExists();
        List<Message> result = new ArrayList<>();
        int num = 0;

        MockMessage lastMsg = null;
        for (MockMessage mockMessage : messages.values()) {
            lastMsg = mockMessage;

            if (end == UIDFolder.LASTUID) {
                if (getMessageCount() != 1 && mockMessage.getMockid() < start) {
                    continue;
                }
            } else {
                if (mockMessage.getMockid() < start || mockMessage.getMockid() > end) {
                    continue;
                }
            }

            mockMessage.setMessageNumber(++num);
            result.add(mockMessage);
        }

        if (end == UIDFolder.LASTUID && result.size() == 0 && lastMsg != null) {
            lastMsg.setMessageNumber(++num);
            result.add(lastMsg);
        }

        return result;
    }

    public synchronized Collection<Message> getByIds(long[] id) {
        checkExists();
        List<Long> idlist = new ArrayList<>();
        for (long value : id) {
            idlist.add(value);
        }

        List<Message> result = new ArrayList<>();
        int num = 0;
        for (MockMessage mockMessage : messages.values()) {
            if (idlist.contains(mockMessage.getMockid())) {
                mockMessage.setMessageNumber(++num);
                result.add(mockMessage);
            }
        }

        return result;
    }

    public synchronized Message getByMsgNum(int msgnum) {
        checkExists();

        List<MockMessage> sms = new ArrayList<>();
        int num = 0;

        for (MockMessage mockMessage : messages.values()) {
            mockMessage.setMessageNumber(++num);
            sms.add(mockMessage);
        }

        if (msgnum - 1 < 0 || msgnum > sms.size()) {
            throw new ArrayIndexOutOfBoundsException(
                "message number (" + msgnum + ") out of bounds ("
                    + sms.size() + ") for " + getFullName());
        }

        return sms.get(msgnum - 1);
    }

    public synchronized List<MailboxFolder> getChildren() {
        checkExists();
        return Collections.unmodifiableList(new ArrayList<>(children));
    }

    public synchronized String getFullName() {
        // checkExists();
        if (isRoot()) {
            return "";
        }

        return parent.isRoot() ? name : parent.getFullName() + SEPARATOR + name;
    }

    public MockMailbox getMailbox() {
        return mailbox;
    }

    public synchronized int getMessageCount() {
        checkExists();
        return messages.size();
    }

    public synchronized Collection<Message> getMessages() {
        checkExists();
        List<Message> result = new ArrayList<>();
        int num = 0;
        for (MockMessage mockMessage : messages.values()) {
            mockMessage.setMessageNumber(++num);
            result.add(mockMessage);
        }
        return result;
    }

    public String getName() {
        return name;
    }

    public synchronized MailboxFolder getOrAddSubFolder(String name)
        throws MessagingException
    {
        // checkExists();

        if (name == null || name.trim().isEmpty()) {
            throw new MessagingException("cannot get or add root folder");
        }

        String[] path = name.split(String.valueOf(SEPARATOR));

        MailboxFolder last = this;
        for (String element : path) {
            if (MockMailbox.isInbox(element)) {
                last = mailbox.getInbox();
            } else {
                checkFolderName(element);
                MailboxFolder mbt = new MailboxFolder(element, mailbox, false);
                mbt.parent = last;

                int index;
                if ((index = last.children.indexOf(mbt)) != -1) {
                    MailboxFolder tmp = last.children.get(index);
                    if (tmp.isExists()) {
                        last = tmp;
                        continue;
                    }
                }

                last.children.add(mbt);

                last = mbt;
            }

        }

        return last;

    }

    public synchronized MailboxFolder getParent() {
        checkExists();
        return parent;
    }

    public synchronized int getSizeInBytes() throws MessagingException {
        checkExists();
        int size = 0;

        for (MockMessage mockMessage : messages.values()) {
            if (mockMessage.getSize() > 0) {
                size += mockMessage.getSize();
            }
        }

        return size;
    }

    public synchronized long getUID(Message msg) {
        checkExists();
        return ((MockMessage) msg).getMockid();
    }

    /**
     * @return the uidValidity
     */
    public synchronized long getUidValidity() {
        checkExists();
        return uidValidity;
    }

    protected synchronized long getUniqueMessageId() {
        return uniqueMessageId;
    }

    @Override
    public int hashCode() {
        int prime = 31;
        int result = 1;
        result = prime * result + Objects.hashCode(name);
        result = prime * result + Objects.hashCode(parent);
        return result;
    }

    public synchronized boolean hasMessages() {
        checkExists();
        return messages.isEmpty();
    }

    public synchronized void invalidateUid() {
        checkExists();
        uidValidity += 10;

        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
            mailboxEventListener.uidInvalidated();
        }
    }

    /**
     * @return the exists
     */
    public boolean isExists() {
        return exists;
    }

    public boolean isInbox() {
        return MockMailbox.isInbox(name);
    }

    public boolean isRoot() {
        return name == null || name.isEmpty() || parent == null;
    }

    public boolean isSimulateError() {
        return simulateError;
    }

    protected boolean isSubscribed() {
        return subscribed;
    }

    public synchronized void markMessageAsDeleted(Message e) throws MessagingException {
        checkExists();
        ((MockMessage) e).setFlag(Flag.DELETED, true);
        // if(mailboxEventListener!=null)
        // mailboxEventListener.messageRemoved(this, ((MockMessage)e), false);
    }

    public synchronized void markMessageAsSeen(Message e) throws MessagingException {
        checkExists();
        ((MockMessage) e).setFlag(Flag.SEEN, true);
        // if(mailboxEventListener!=null)
        // mailboxEventListener.messageRemoved(this, ((MockMessage)e), false);
    }

    @Override
    public void onFlagChange(MockMessage msg, Flags flags, boolean set) {
        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
            mailboxEventListener.messageChanged(this, msg, false, true);
        }

        if (messages.size() > 0 && messages.get(msg.getMockid()) != null) {
            try {
                if (set && messages.get(msg.getMockid()).getFlags().contains(flags)) {
                    return;
                }

                if (set && !messages.get(msg.getMockid()).getFlags().contains(flags)) {
                    messages.get(msg.getMockid()).setFlags(flags, set);
                }

                if (!set && messages.get(msg.getMockid()).getFlags().contains(flags)) {
                    messages.get(msg.getMockid()).setFlags(flags, set);
                }

                if (!set && !messages.get(msg.getMockid()).getFlags().contains(flags)) {
                    return;
                }
            } catch (MessagingException e) {
                throw new IllegalStateException(e);
            }
        }
    }

    public synchronized void removeMailboxEventListener(MailboxEventListener l) {
        if (l != null) {
            mailboxEventListeners.remove(l);
        }
    }

    public synchronized void renameFolder(String newName) {
        checkExists();
        checkFolderName(this.name);
        checkFolderName(newName);
        String tmpOldName = name;

        name = newName;

        for (MailboxEventListener mailboxEventListener : mailboxEventListeners) {
            mailboxEventListener.folderRenamed(tmpOldName, this);
        }

        // TODO purge old folders, exists =false

        // TODO notify children?
        /*for (MailboxFolder mf: children) {
        	renameFolder(mf.name); //do not really change name of children, just notify because parent changes
        }*/
    }

    public Collection<Message> search(SearchTerm term, Message[] msgsToSearch) {
        List<Message> result = new ArrayList<>();
        List<Message> msgsToSearchL = new ArrayList<>();

        if (msgsToSearch != null) {
            msgsToSearchL.addAll(Arrays.asList(msgsToSearch));
        }

        for (Message msg : getMessages()) {
            if (term != null && term.match(msg)) {
                if (msgsToSearch == null || msgsToSearchL.contains(msg)) {
                    result.add(msg);
                }
            }
        }
        return result;
    }

    public void setSimulateError(boolean simulateError) {
        this.simulateError = simulateError;
    }

    void setSubscribed(boolean subscribed) {
        this.subscribed = subscribed;
    }
    
}
