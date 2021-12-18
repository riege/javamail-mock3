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
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import javax.mail.FetchProfile;
import javax.mail.Flags;
import javax.mail.Flags.Flag;
import javax.mail.Folder;
import javax.mail.FolderClosedException;
import javax.mail.FolderNotFoundException;
import javax.mail.Message;
import javax.mail.MessagingException;
import javax.mail.Quota;
import javax.mail.event.ConnectionEvent;
import javax.mail.event.FolderEvent;
import javax.mail.event.MailEvent;
import javax.mail.event.MessageChangedEvent;
import javax.mail.internet.MimeMessage;
import javax.mail.search.SearchTerm;

import com.sun.mail.iap.ProtocolException;
import com.sun.mail.iap.Response;
import com.sun.mail.imap.AppendUID;
import com.sun.mail.imap.IMAPFolder;
import com.sun.mail.imap.ResyncData;
import com.sun.mail.imap.SortTerm;

import static com.github.golovnin.javamail.mock3.MockMailbox.isInbox;

public class IMAPMockFolder extends IMAPFolder implements
    MailboxFolder.MailboxEventListener
{

    private static final int ABORTING = 2; // IDLE command aborting
    private static final int IDLE = 1; // IDLE command in effect
    private static final int RUNNING = 0; // not doing IDLE command

    private final MailboxFolder mailboxFolder;
    private final IMAPMockStore store;
    private final UUID objectId = UUID.randomUUID();
    private final Semaphore idleLock = new Semaphore(0, true);

    private int idleState = RUNNING;
    private volatile boolean opened = false;
    private int openMode;

    IMAPMockFolder(IMAPMockStore store, MailboxFolder mailboxFolder) {
        super("DUMMY_NAME_WHICH_MUST_NOT_BE_VISIBLE", MailboxFolder.SEPARATOR, store, false);
        this.mailboxFolder = mailboxFolder;
        this.mailboxFolder.addMailboxEventListener(this);
        this.store = store;
    }

    private synchronized void abortIdle() {
        if (idleState == IDLE) {
            idleState = ABORTING;
            idleLock.release();
        }
    }

    @Override
    public void appendMessages(Message[] msgs) throws MessagingException {
        abortIdle();
        checkExists();
        // checkOpened();
        // checkWriteMode();
        for (Message m : msgs) {
            mailboxFolder.add((MimeMessage) m);
        }
    }

    @Override
    public synchronized AppendUID[] appendUIDMessages(Message[] msgs) throws MessagingException {
        abortIdle();
        checkExists();
        // checkOpened();
        // checkWriteMode();
        AppendUID[] uids = new AppendUID[msgs.length];
        for (int i = 0; i < msgs.length; i++) {
            MockMessage mockMessage = (MockMessage) mailboxFolder.add((MimeMessage) msgs[i]);
            uids[i] = new AppendUID(mailboxFolder.getUidValidity(), mockMessage.getMockid());
        }

        return uids;
    }

    @Override
    protected void checkClosed() {
        if (opened) {
            throw new IllegalStateException(
                "This operation is not allowed on an open folder:"
                    + getFullName() + " (" + objectId + ")");
        }
    }

    @Override
    protected void checkExists() throws MessagingException {
        if (!exists()) {
            throw new FolderNotFoundException(this, getFullName() + " not found");
        }
    }

    @Override
    protected void checkOpened() throws FolderClosedException {
        if (!opened) {
            throw new IllegalStateException(
                "This operation is not allowed on a closed folder: "
                    + getFullName() + " (" + objectId + ')');
        }
    }

    private void checkWriteMode() {
        if (openMode != Folder.READ_WRITE) {
            throw new IllegalStateException("Folder " + getFullName()
                + " is readonly (" + objectId + ')');
        }
    }

    @Override
    public synchronized void close(boolean expunge) throws MessagingException {
        abortIdle();
        checkOpened();
        checkExists();

        if (expunge) {
            expunge();
        }

        opened = false;
        notifyConnectionListeners(ConnectionEvent.CLOSED);
    }

    @Override
    public synchronized void copyMessages(Message[] msgs, Folder folder)
        throws MessagingException
    {
        abortIdle();
        checkOpened();
        checkExists();
        if (msgs == null || folder == null || msgs.length == 0) {
            return;
        }

        if (!folder.exists()) {
            throw new FolderNotFoundException(folder.getFullName()
                + " does not exist", folder);
        }

        folder.appendMessages(msgs);
    }

    @Override
    public synchronized AppendUID[] copyUIDMessages(Message[] msgs,
        Folder folder) throws MessagingException
    {
        abortIdle();
        checkExists();
        checkOpened();
        if (msgs == null || folder == null || msgs.length == 0) {
            return null;
        }

        AppendUID[] uids = new AppendUID[msgs.length];

        for (int i = 0; i < msgs.length; i++) {
            MockMessage mockMessage = (MockMessage) mailboxFolder.add((MimeMessage) msgs[i]);
            uids[i] = new AppendUID(mailboxFolder.getUidValidity(), mockMessage.getMockid());
        }

        return uids;
    }

    @Override
    public synchronized boolean create(int type) throws MessagingException {
        abortIdle();
        if (exists()) {
            return true;
        }

        mailboxFolder.create();
        notifyFolderListeners(FolderEvent.CREATED);
        return mailboxFolder.isExists();
    }

    @Override
    public synchronized boolean delete(boolean recurse) throws MessagingException {
        abortIdle();
        checkExists();
        checkClosed();
        mailboxFolder.deleteFolder(recurse);
        notifyFolderListeners(FolderEvent.DELETED);
        return true;
    }

    @Override
    public Object doCommand(ProtocolCommand cmd) throws MessagingException {
        throw new MessagingException(
            "no protocol for mock class - you should never see " +
                "this exception. Please file a bugrfeport and include stacktrace");
    }

    @Override
    public Object doCommandIgnoreFailure(ProtocolCommand cmd)
        throws MessagingException
    {
        throw new MessagingException("no protocol for mock class - " +
            "you should never see this exception. " +
            "Please file a bugrfeport and include stacktrace");
    }

    @Override
    public Object doOptionalCommand(String err, ProtocolCommand cmd)
        throws MessagingException
    {
        throw new MessagingException("Optional command not supported: " + err);
    }

    @Override
    protected Object doProtocolCommand(ProtocolCommand cmd)
        throws ProtocolException
    {
        throw new ProtocolException("no protocol for mock class - " +
            "you should never see this exception. " +
            "Please file a bugrfeport and include stacktrace");
    }

    @Override
    public synchronized boolean exists() throws MessagingException {
        abortIdle();
        return mailboxFolder.isExists();
    }

    @Override
    public synchronized Message[] expunge() throws MessagingException {
        abortIdle();
        checkExists();
        checkOpened();
        checkWriteMode();

        Message[] removed = wrap(mailboxFolder.expunge());

        if (removed.length > 0) {
            notifyMessageRemovedListeners(true, removed);
        }

        return removed;
    }

    @Override
    public synchronized Message[] expunge(Message[] msgs)
        throws MessagingException
    {
        abortIdle();
        checkExists();
        checkOpened();
        checkWriteMode();
        Message[] removed = wrap(mailboxFolder.expunge(msgs));

        if (removed.length > 0) {
            notifyMessageRemovedListeners(true, removed);
        }

        return removed;
    }

    @Override
    public synchronized void fetch(Message[] msgs, FetchProfile fp)
        throws MessagingException
    {
        abortIdle();
        // do nothing more
    }

    @Override
    public void folderCreated(MailboxFolder mf) {
        // ignore
    }

    @Override
    public void folderDeleted(MailboxFolder mf) {
        // ignore
    }

    @Override
    public void folderRenamed(String from, MailboxFolder to) {
        // ignore
    }

    @Override
    public synchronized void forceClose() throws MessagingException {
        close(false);
    }

    @Override
    public synchronized String[] getAttributes() throws MessagingException {
        checkExists();
        return new String[0];
    }

    @Override
    public synchronized int getDeletedMessageCount() throws MessagingException {
        abortIdle();
        checkExists();
        if (!opened) {
            return -1;
        }

        return mailboxFolder.getByFlags(new Flags(Flags.Flag.DELETED), false).size();
    }

    @Override
    public synchronized Folder getFolder(String name) throws MessagingException {
        abortIdle();
        // checkExists();

        if (isInbox(name)) {
            return new IMAPMockFolder(store, mailboxFolder.getMailbox().getInbox());
        }

        return new IMAPMockFolder(store, mailboxFolder.getOrAddSubFolder(name));
    }

    @Override
    public synchronized String getFullName() {
        return mailboxFolder.getFullName();
    }

    @Override
    public long getHighestModSeq() throws MessagingException {
        throw new MessagingException("CONDSTORE not supported");
    }

    @Override
    public synchronized Message getMessage(int msgnum) throws MessagingException {
        abortIdle();
        checkExists();
        checkOpened();
        return new MockMessage(mailboxFolder.getByMsgNum(msgnum), this);
    }

    @Override
    public synchronized Message getMessageByUID(long uid)
        throws MessagingException
    {
        abortIdle();
        checkExists();
        checkOpened();
        Message message = mailboxFolder.getById(uid);
        return message != null ? new MockMessage(message, this) : null;
    }

    @Override
    public synchronized int getMessageCount() throws MessagingException {
        abortIdle();
        checkExists();
        return mailboxFolder.getMessageCount();
    }

    @Override
    public synchronized Message[] getMessages() throws MessagingException {
        abortIdle();
        checkExists();
        return wrap(mailboxFolder.getMessages());
    }

    @Override
    public synchronized Message[] getMessages(int low, int high)
        throws MessagingException
    {
        abortIdle();
        checkExists();
        checkOpened();
        List<Message> messages = new ArrayList<>();
        for (int i = low; i <= high; i++) {
            Message m = mailboxFolder.getByMsgNum(i);
            messages.add(new MockMessage(m, this));
        }
        return messages.toArray(new Message[messages.size()]);
    }

    @Override
    public synchronized Message[] getMessagesByUID(long start, long end)
        throws MessagingException
    {
        abortIdle();
        checkExists();
        checkOpened();
        return wrap(mailboxFolder.getByIds(start, end));
    }

    @Override
    public synchronized Message[] getMessagesByUID(long[] uids)
        throws MessagingException
    {
        abortIdle();
        checkExists();
        checkOpened();
        return wrap(mailboxFolder.getByIds(uids));
    }

    @Override
    public Message[] getMessagesByUIDChangedSince(long start,
        long end, long modseq) throws MessagingException
    {
        throw new MessagingException("CONDSTORE not supported");
    }

    @Override
    public synchronized String getName() {
        return mailboxFolder.getName();
    }

    @Override
    public synchronized int getNewMessageCount() throws MessagingException {
        abortIdle();
        checkExists();
        return mailboxFolder.getByFlags(new Flags(Flag.RECENT), true).size();
    }

    @Override
    public synchronized Folder getParent() throws MessagingException {
        checkExists();
        if (mailboxFolder.getParent() == null) {
            throw new MessagingException("no parent, is already default root");
        }

        return new IMAPMockFolder(store, mailboxFolder.getParent());
    }

    @Override
    public Flags getPermanentFlags() {
        return null;
    }

    @Override
    public Quota[] getQuota() throws MessagingException {
        throw new MessagingException("QUOTA not supported");
    }

    @Override
    public synchronized char getSeparator() throws MessagingException {
        abortIdle();
        return MailboxFolder.SEPARATOR;
    }

    @Override
    public Message[] getSortedMessages(SortTerm[] term)
        throws MessagingException
    {
        throw new MessagingException("SORT not supported");
    }

    @Override
    public Message[] getSortedMessages(SortTerm[] term, SearchTerm sterm)
        throws MessagingException
    {
        throw new MessagingException("SORT not supported");
    }

    @Override
    public synchronized int getType() throws MessagingException {
        // checkExists();
        return mailboxFolder.isRoot()
             ? HOLDS_FOLDERS
             : HOLDS_MESSAGES | HOLDS_FOLDERS;
    }

    @Override
    public synchronized long getUID(Message message) throws MessagingException {
        abortIdle();
        return mailboxFolder.getUID(message);
    }

    @Override
    public synchronized long getUIDNext() throws MessagingException {
        abortIdle();
        return mailboxFolder.getUniqueMessageId() + 10;
    }

    @Override
    public synchronized long getUIDValidity() throws MessagingException {
        abortIdle();
        return mailboxFolder.getUidValidity();
    }

    @Override
    public synchronized int getUnreadMessageCount() throws MessagingException {
        abortIdle();
        checkExists();
        return mailboxFolder.getByFlags(new Flags(Flags.Flag.SEEN), false).size();
    }

    @Override
    public void handleResponse(Response r) {
        throw new UnsupportedOperationException("not implemented/should not happen");
    }

    @Override
    public boolean hasNewMessages() throws MessagingException {
        checkExists();
        return getNewMessageCount() > 0;
    }

    @Override
    public Map<String, String> id(Map<String, String> clientParams)
        throws MessagingException
    {
        checkOpened();
        return store.id(clientParams);
    }

    @Override
    public void idle(boolean once) throws MessagingException {
        synchronized (this) { // blocks until folder lock available
            checkOpened();
            if (idleState == RUNNING) {
                idleState = IDLE;
                // this thread is now idle
            } else {
                // another thread must be currently idle
                return;
            }
        }

        // give up folder lock
        try {
            while (idleState != ABORTING && opened && mailboxFolder.isExists()) {
                idleLock.acquire(); // wait for folder actions, like new mails

                if (once) {
                    break;
                }
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            // thread interrupted, set idleState to running and return
        } finally {
            idleState = RUNNING;
        }
    }

    @Override
    public synchronized boolean isOpen() {
        return opened;
    }

    @Override
    public synchronized boolean isSubscribed() {
        abortIdle();
        return mailboxFolder.isSubscribed();
    }

    @Override
    public Folder[] list(String pattern) throws MessagingException {
        abortIdle();
        checkExists();
        // TODO evaluate pattern
        return mailboxFolder.getChildren()
            .stream()
            .filter(MailboxFolder::isExists)
            .map(mf -> new IMAPMockFolder(store, mf))
            .toArray(Folder[]::new);
    }

    @Override
    public Folder[] listSubscribed(String pattern) throws MessagingException {
        abortIdle();
        checkExists();
        // TODO evaluate pattern
        return mailboxFolder.getChildren()
            .stream()
            .filter(MailboxFolder::isExists)
            .filter(MailboxFolder::isSubscribed)
            .map(mf -> new IMAPMockFolder(store, mf))
            .toArray(Folder[]::new);
    }

    @Override
    public void messageAdded(MailboxFolder mf, MockMessage msg) {
        notifyMessageAddedListeners(new Message[] { msg });
        idleLock.release();
    }

    @Override
    public void messageChanged(MailboxFolder mf, MockMessage msg,
        boolean headerChanged, boolean flagsChanged)
    {
        notifyMessageChangedListeners(MessageChangedEvent.FLAGS_CHANGED, msg);
        idleLock.release();
    }

    @Override
    public void messageExpunged(MailboxFolder mf, MockMessage msg, boolean removed) {
        idleLock.release();
    }

    @Override
    public synchronized void open(int mode) throws MessagingException {
        checkClosed();
        checkExists();
        opened = true;
        openMode = mode;
        notifyConnectionListeners(ConnectionEvent.OPENED);
    }

    @Override
    public synchronized List<MailEvent> open(int mode, ResyncData rd)
        throws MessagingException
    {
        if (rd == null) {
            open(mode);
            return null;
        }

        throw new MessagingException("CONDSTORE and QRESYNC not supported");
    }

    @Override
    public synchronized boolean renameTo(Folder f) throws MessagingException {
        abortIdle();
        checkClosed(); // insure that we are closed.
        checkExists();
        if (f.getStore() != store) {
            throw new MessagingException("Can't rename across Stores");
        }

        mailboxFolder.renameFolder(f.getName());
        notifyFolderRenamedListeners(f);
        return true;
    }

    @Override
    public synchronized Message[] search(SearchTerm term) throws MessagingException {
        return search(term, null);
    }

    @Override
    public synchronized Message[] search(SearchTerm term, Message[] msgs)
        throws MessagingException
    {
        abortIdle();
        checkOpened();
        return wrap(mailboxFolder.search(term, msgs));
    }

    @Override
    public synchronized void setFlags(Message[] msgs, Flags flag, boolean value)
        throws MessagingException
    {
        abortIdle();
        checkOpened();

        for (Message message : msgs) {
            Message m = mailboxFolder.getById(((MockMessage) message).getMockid());
            if (m != null) {
                m.setFlags(flag, value);
            }
        }
    }

    @Override
    public void setQuota(Quota quota) throws MessagingException {
        throw new MessagingException("QUOTA not supported");
    }

    @Override
    public synchronized void setSubscribed(boolean subscribe)
        throws MessagingException
    {
        abortIdle();
        mailboxFolder.setSubscribed(subscribe);
    }

    @Override
    public void uidInvalidated() {
        // ignore
    }

    @Override
    public synchronized void moveMessages(Message[] msgs, Folder folder)
        throws MessagingException
    {
        abortIdle();
        checkOpened();
        checkExists();
        if (msgs == null || folder == null || msgs.length == 0) {
            return;
        }
        if (folder.getStore() != store) {
            // destination is a different store.
            throw new MessagingException("Can't move to a different store");
        }
        if (!folder.exists()) {
            throw new FolderNotFoundException(folder.getFullName()
                + " does not exist", folder);
        }

        folder.appendMessages(msgs);
        mailboxFolder.delete(msgs);
    }

    @Override
    public synchronized AppendUID[] moveUIDMessages(Message[] msgs,
        Folder folder) throws MessagingException
    {
        abortIdle();
        checkOpened();
        checkExists();
        if (msgs == null || folder == null || msgs.length == 0) {
            return null;
        }
        if (folder.getStore() != store) {
            // destination is a different store.
            throw new MessagingException("Can't move to a different store");
        }
        if (!(folder instanceof IMAPFolder)) {
            throw new IllegalArgumentException(
                "folder must be of type IMAPFolder");
        }
        if (!folder.exists()) {
            throw new FolderNotFoundException(folder.getFullName()
                + " does not exist", folder);
        }

        IMAPFolder imapFolder = (IMAPFolder) folder;
        AppendUID[] result = imapFolder.appendUIDMessages(msgs);
        mailboxFolder.delete(msgs);
        return result;
    }

    private Message[] wrap(Collection<Message> msgs) throws MessagingException {
        int i = 0;
        Message[] ret = new Message[msgs.size()];
        for (Message m : msgs) {
            ret[i++] = new MockMessage(m, this);
        }
        return ret;
    }

}
