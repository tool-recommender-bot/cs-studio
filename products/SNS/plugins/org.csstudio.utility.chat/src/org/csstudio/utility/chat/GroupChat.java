/*******************************************************************************
 * Copyright (c) 2011 Oak Ridge National Laboratory.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 ******************************************************************************/
package org.csstudio.utility.chat;

import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;
import org.eclipse.osgi.util.NLS;
import org.jivesoftware.smack.AccountManager;
import org.jivesoftware.smack.Chat;
import org.jivesoftware.smack.ChatManagerListener;
import org.jivesoftware.smack.ConnectionConfiguration;
import org.jivesoftware.smack.PacketListener;
import org.jivesoftware.smack.SmackConfiguration;
import org.jivesoftware.smack.XMPPConnection;
import org.jivesoftware.smack.packet.Message;
import org.jivesoftware.smack.packet.Packet;
import org.jivesoftware.smack.util.StringUtils;
import org.jivesoftware.smackx.filetransfer.FileTransferListener;
import org.jivesoftware.smackx.filetransfer.FileTransferManager;
import org.jivesoftware.smackx.filetransfer.FileTransferRequest;
import org.jivesoftware.smackx.filetransfer.IncomingFileTransfer;
import org.jivesoftware.smackx.filetransfer.OutgoingFileTransfer;
import org.jivesoftware.smackx.muc.DefaultParticipantStatusListener;
import org.jivesoftware.smackx.muc.MultiUserChat;
import org.jivesoftware.smackx.muc.Occupant;

/** A place for nerds to chat
 * 
 *  <p>Uses XMPP via the SMACK library.
 *  For one, using that with group chats was
 *  not immediately obvious.
 *  In addition, this layer would allow replacment
 *  of the protocol.
 *  
 *  @author Kay Kasemir
 */
public class GroupChat
{
	/** XMPP 'resource' used to connect */
	private static final String XMPP_RESOURCE = "css"; //$NON-NLS-1$

	/** Connection to server */
	final private XMPPConnection connection;

	/** Smack file transfer manager for connection */
	private FileTransferManager file_manager;
	
	/** Name of chat group */
	final private String group;
	
	/** Connected chat group */
	private MultiUserChat chat;
	
	/** Nerds in the chat group */
	final private Set<Person> nerds = new HashSet<Person>();
	
	/** Listeners to the {@link GroupChat} */
	private GroupChatListener listener = null;

	/** Our name used in this group chat */
	private String user;

	
	/** Initialize
	 *  @param host XMMP server host
	 *  @param group Chat group to join
	 *  @throws Exception on error
	 */
	public GroupChat(final String host, final String group) throws Exception
    {
		// Avoid message "couldn't setup local SOCKS5 proxy on port" on disconnect
		SmackConfiguration.setLocalSocks5ProxyEnabled(false);
		// Connect to host, port
		final ConnectionConfiguration config = new ConnectionConfiguration(host, 5222);
		connection = new XMPPConnection(config);
		connection.connect();
		this.group = group;
    }

	/** @param listener Listener to add */
	public void addListener(final GroupChatListener listener)
	{
		if (this.listener != null)
			throw new Error("Can only have one listener"); //$NON-NLS-1$
		this.listener = listener;
	}
	
	/** Connect to the server
	 *  @param user User name
	 *  @param password Password
	 *  @throws Exception on error
	 */
    public void connect(final String user, final String password) throws Exception
    {
    	this.user = user;
    	
		// Try to create account.
		// If account already exists, this will fail.
		final AccountManager accounts = connection.getAccountManager();
		if (accounts.supportsAccountCreation())
		{
			try
			{
				accounts.createAccount(user, password);
			}
			catch (final Exception ex)
			{
				// Ignore
			}
		}
		
		// Log on
		connection.login(user, password, XMPP_RESOURCE);

	    // Join chat group
		chat = new MultiUserChat(connection, group);
		chat.join(user);
		
		// Listen to nerd changes
		chat.addParticipantStatusListener(new DefaultParticipantStatusListener()
		{
			@Override
			public void joined(final String participant)
			{
				final String nick = StringUtils.parseResource(participant);
				final Occupant info = chat.getOccupant(participant);
				synchronized (nerds)
                {
					nerds.add(new Person(nick, info.getJid()));
                }
				fireNerdAlert();
			}

			@Override
			public void left(final String participant)
			{
				final String nick = StringUtils.parseResource(participant);
				synchronized (nerds)
                {
					nerds.remove(nick);
                }
				fireNerdAlert();
			}
		});

		// Determine who's there initially
		synchronized (nerds)
        {
			// Add ourself, which we don't always seem to get from server
			nerds.add(new Person(user, connection.getUser()));
			// Then query server, which might include an update for ourself
			final Iterator<String> occupants = chat.getOccupants();
			while (occupants.hasNext())
			{
				final String occupant = occupants.next();
				final Occupant info = chat.getOccupant(occupant);
				final String nick = StringUtils.parseResource(occupant);
				nerds.add(new Person(nick, info.getJid()));
			}
        }
		fireNerdAlert();
		
		// Listen to messages
		chat.addMessageListener(new PacketListener()
		{
			@Override
			public void processPacket(final Packet packet)
			{
				if (packet instanceof Message)
				{
					final Message message = (Message) packet;
					String nick = StringUtils.parseResource(message.getFrom());
					if (nick.length() <= 0)
						nick = Messages.UserSERVER;
					final String body = message.getBody();
					if (listener != null)
						listener.receive(nick, nick.equals(user), body);
				}
			}
		});
		
		// Listen to invitations
		connection.getChatManager().addChatListener(new ChatManagerListener()
		{
			@Override
			public void chatCreated(final Chat chat, final boolean createdLocally)
			{
				if (createdLocally)
					return;
				if (listener != null)
				{
					final String from = chat.getParticipant();
					final IndividualChatGUI gui = listener.receivedInvitation(from);
					if (gui == null)
					{	// Politely refuse
						try
						{
							chat.sendMessage(NLS.bind(Messages.DeclineChatFmt, user));
						}
						catch (Exception ex)
						{
							// Ignore
						}
					}
					else
						listener.startIndividualChat(from, new IndividualChat(user, chat));
				}
			}
		});

		// Listen to received files
		file_manager = new FileTransferManager(connection);
		file_manager.addFileTransferListener(new FileTransferListener()
		{
			@Override
			public void fileTransferRequest(final FileTransferRequest request)
			{
				File file = null;
				if (listener != null)
					file = listener.receivedFile(request.getRequestor(), request.getFileName());
				if (file != null)
				{
					final IncomingFileTransfer transfer = request.accept();
					receiveFile(transfer, file);
				}
				else
					request.reject();
			}
		});
    }
    
    /** Receive a file, displaying progress in Eclipse Job
     *  @param transfer {@link IncomingFileTransfer}
     *  @param file {@link File} to create
     */
    protected void receiveFile(final IncomingFileTransfer transfer, final File file)
    {
    	new Job("Receive File") //$NON-NLS-1$
    	{
			@Override
            protected IStatus run(final IProgressMonitor monitor)
            {
				// TODO Receiving file from Pidgin does not work.
				// Sending to Pidgin is OK, but not receiving
				// Piding proxy settings?
				monitor.beginTask("Receive file", IProgressMonitor.UNKNOWN);
				try
				{
					System.out.println("Receiving " + file.getPath());
					transfer.recieveFile(file);
					System.out.println("started...");
					while (! transfer.isDone())
					{
						if (monitor.isCanceled())
						{
							transfer.cancel();
							break;
						}
						System.out.println("received " + transfer.getAmountWritten());
						monitor.subTask(NLS.bind("Received {0} of {1} bytes",
								transfer.getAmountWritten(),
								transfer.getFileSize()));
						Thread.sleep(1000);
					}
				}
				catch (Exception ex)
				{
					ex.printStackTrace();
					monitor.subTask("Error saving to " + file.getName() + ": " + ex.getMessage());
					while (! monitor.isCanceled())
					{
						try
						{
							Thread.sleep(1000);
						}
						catch (Exception e)
						{
							break;
						}
					}
				}
				monitor.done();
	            return Status.OK_STATUS;
            }
    	}.schedule();
     }

	/** Notify listeners about current nerd list */
	private void fireNerdAlert()
    {
		final Person[] array;
		synchronized (nerds)
        {
			array = nerds.toArray(new Person[nerds.size()]);
        }
		Arrays.sort(array);
		if (listener != null)
			listener.groupMemberUpdate(array);
    }

    /** @param text Message to send to the chat
     *  @throws Exception on error
     */
	public void send(final String text) throws Exception
    {
    	chat.sendMessage(text);
    }
	
	/** Disconnect from chat server */
    public void disconnect()
    {
    	listener = null;
    	file_manager = null;
    	try
    	{
	    	if (chat != null)
	    		chat.leave();
	    	connection.disconnect();
    	}
    	catch (Throwable ex)
    	{
    		// Ignore, shutting down
    	}
    }

    /** Start individual chat
     *  @param person A {@link Person} in the chat group
     *  @return New {@link IndividualChat}
     */
	public IndividualChat createIndividualChat(final Person person)
    {
		// When we contact the person via the in-room nickname,
		// that "works" but is not the same as contacting the
		// person directly, as done by other chat clients like "pidgin".

		// So try direct
		String address = person.getAddress();
		// and fall back to in-room address
		if (address == null  ||  address.isEmpty())
			address = group + "/" + person.getName(); //$NON-NLS-1$
		final Chat new_chat = chat.createPrivateChat(address, null);
		return new IndividualChat(user, new_chat);
    }

	/** Start a file transfer
	 *  @param person Recipient of the file
	 *  @param file The File
	 *  @throws Exception on error
	 */
	public void sendFile(final Person person, final File file) throws Exception
    {
		if (file_manager == null)
			return;
		final OutgoingFileTransfer transfer = file_manager.createOutgoingFileTransfer(person.getAddress());
        transfer.sendFile(file, file.getName());
        // Eclipse Job to monitor and maybe cancel the transfer
        new Job(Messages.JobSendingFile)
        {
			@Override
            protected IStatus run(final IProgressMonitor monitor)
            {
				monitor.beginTask(file.getName(), IProgressMonitor.UNKNOWN);
				while (! transfer.isDone())
				{
					if (monitor.isCanceled())
					{
						transfer.cancel();
						break;
					}
					monitor.subTask(NLS.bind(Messages.JobSendingFileUpdateFmt,
							transfer.getBytesSent(), transfer.getFileSize()));
					try
                    {
	                    Thread.sleep(1000);
                    }
                    catch (InterruptedException e)
                    {
	                    // ignore
                    }
				}
				monitor.done();
	            return Status.OK_STATUS;
            }
        }.schedule();
    }
}
