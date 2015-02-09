/**
Copyright 2013 Luciano Zu project Ardulink http://www.ardulink.org/

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

@author Luciano Zu
*/

package org.zu.ardulink.protocol;

import java.util.logging.Logger;

/**
 * [ardulinktitle] [ardulinkversion]
 * A simple ReplyMessageCallback implementation just to log MessageInfo values.
 * @author Luciano Zu project Ardulink http://www.ardulink.org/
 * @see ReplyMessageCallback
 * [adsense]
 *
 */
public class LoggerReplyMessageCallback implements ReplyMessageCallback {

	private static Logger logger = Logger.getLogger(LoggerReplyMessageCallback.class.getName());

	@Override
	public void replyInfo(MessageInfo messageInfo) {
		logger.info("Reply Message has arrived");
		logger.info("ID:" + messageInfo.getMessageID());
		logger.info("Message sent: " + messageInfo.getMessageSent());
		logger.info("Message received: " + messageInfo.getMessageReceived());
		logger.info("Result: " + messageInfo.getReply());
	}

}
