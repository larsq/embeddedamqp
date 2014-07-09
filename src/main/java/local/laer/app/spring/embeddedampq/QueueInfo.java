/*
 * The MIT License
 *
 * Copyright 2014 Lars Eriksson (larsq.eriksson@gmail.com).
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

package local.laer.app.spring.embeddedampq;

import com.rabbitmq.client.GetResponse;
import java.util.Optional;
import java.util.Queue;

/**
 *
 * @author Lars Eriksson (larsq.eriksson@gmail.com)
 */
public class QueueInfo {
    private final SimpleAmqpMessageContainer container;
    private int messageCount;
    private int consumerCount;
    private final String queue;

    public QueueInfo(SimpleAmqpMessageContainer container, String queue) {
        this.container = container;
        this.queue = queue;
        
        QueueInfo.this.refresh();
    }
    
    public String getQueue() {
        return queue;
    }

    public int getMessageCount() {
        return messageCount;
    }

    void refresh() {
          messageCount = container.countUnread(queue);
          consumerCount = container.countConsumers(queue);
    }
    public int getConsumerCount() {
        return consumerCount;
    }

    void purge() {
        container.purgeQueue(queue);
    }
    

    Optional<GetResponse> receive() {
        Optional<Message> optionalMessage = container.messages(queue).map(q->q.poll());
        
        if(!optionalMessage.isPresent()) {
            return Optional.empty();
        }
        
        Message m = optionalMessage.get();
        
        refresh();
        
        GetResponse response = new GetResponse(m.getEnvelope(), m.getBasicProperties(), m.getPayload(), messageCount);
        return Optional.of(response);
    }
}
