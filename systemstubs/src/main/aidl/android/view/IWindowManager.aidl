/*
** Copyright 2006, The Android Open Source Project
**
** Licensed under the Apache License, Version 2.0 (the "License");
** you may not use this file except in compliance with the License.
** You may obtain a copy of the License at
**
**     http://www.apache.org/licenses/LICENSE-2.0
**
** Unless required by applicable law or agreed to in writing, software
** distributed under the License is distributed on an "AS IS" BASIS,
** WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
** See the License for the specific language governing permissions and
** limitations under the License.
*/
package android.view;

/**
 * System private interface to the window manager.
 */
interface IWindowManager
{
    /** Returns {@code true} if this binder is a registered window token. */
    boolean isWindowToken(in IBinder binder);

    /**
     * Adds window token for a given type.
     *
     * @param token Token to be registered.
     * @param type Window type to be used with this token.
     * @param displayId The ID of the display where this token should be added.
     * @param options A bundle used to pass window-related options.
     */
    void addWindowToken(IBinder token, int type, int displayId, in Bundle options);
}