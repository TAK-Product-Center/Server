/*
 * Copyright (c) 2017 Guus der Kinderen. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */

package nl.goodbytes.xmpp.xep0363;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import org.xmpp.packet.JID;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * A manager of HTTP slots.
 *
 * @author Guus der Kinderen, guus@goodbytes.nl
 */
// TODO: quick 'n dirty singleton. Is this the best choice?
// TODO: persist internal state to allow for restart survival.
public class SlotManager
{
    private static SlotManager INSTANCE = null;
    private final Cache<UUID, Slot> slots;
    private long maxFileSize = 20 * 1024 * 1024;
    private long putExpiryValue = 5;
    private TimeUnit putExpiryUnit = TimeUnit.MINUTES;


    private SlotManager()
    {
        slots = CacheBuilder.newBuilder()
                .expireAfterWrite( putExpiryValue, putExpiryUnit )
                .build();
    }

    public synchronized static SlotManager getInstance()
    {
        if ( INSTANCE == null )
        {
            INSTANCE = new SlotManager();
        }

        return INSTANCE;
    }

    public long getMaxFileSize()
    {
        return maxFileSize;
    }

    public Slot getSlot( JID from, String fileName, long fileSize, String contentType ) throws TooLargeException
    {
        if ( maxFileSize > 0 && fileSize > maxFileSize )
        {
            throw new TooLargeException( fileSize, maxFileSize );
        }

        final Slot slot = new Slot( from, fileName, fileSize, contentType );

        slots.put( slot.getUuid(), slot );
        return slot;
    }

    public Slot consumeSlotForPut( UUID uuid )
    {
        final Slot slot = slots.getIfPresent( uuid );
        if ( slot != null )
        {
            slots.invalidate( uuid );
        }

        return slot;
    }
}
