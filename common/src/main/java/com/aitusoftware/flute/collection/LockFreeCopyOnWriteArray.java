/*
 * Copyright 2016 Aitu Software Limited.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.aitusoftware.flute.collection;

import com.aitusoftware.flute.compatibility.Consumer;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicReference;

public final class LockFreeCopyOnWriteArray<T>
{
    private AtomicReference<Object[]> holder = new AtomicReference<>(new Object[0]);



    @SuppressWarnings("unchecked")
    public void add(final T item)
    {
        while(true)
        {
            final Object[] existing = holder.get();
            final Object[] updated = Arrays.copyOf(existing, existing.length + 1);

            updated[existing.length] = item;
            if(holder.compareAndSet(existing, updated))
            {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void remove(final T item)
    {
        while(true)
        {
            final Object[] existing = holder.get();
            final Object[] updated = new Object[existing.length - 1];
            int writeIndex = 0;
            for(int i = 0; i < existing.length; i++)
            {
                if(existing[i] != item)
                {
                    updated[writeIndex++] = existing[i];
                }
            }
            if(holder.compareAndSet(existing, updated))
            {
                return;
            }
        }
    }

    @SuppressWarnings("unchecked")
    public void forEach(final Consumer<T> consumer)
    {
        final Object[] items = holder.get();
        for (int i = 0; i < items.length; i++)
        {
            consumer.accept((T) items[i]);
        }
    }
}
