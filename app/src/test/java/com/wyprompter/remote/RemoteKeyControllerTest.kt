package com.wyprompter.remote

import android.view.KeyEvent
import com.wyprompter.remote.RemoteKeyController.Action
import com.wyprompter.remote.RemoteKeyController.Companion.translateKey
import org.junit.Assert.assertEquals
import org.junit.Test

class RemoteKeyControllerTest {

    @Test
    fun `volume up maps to next`() {
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_VOLUME_UP))
    }

    @Test
    fun `volume down maps to prev`() {
        assertEquals(Action.PREV, translateKey(KeyEvent.KEYCODE_VOLUME_DOWN))
    }

    @Test
    fun `media next maps to next`() {
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_MEDIA_NEXT))
    }

    @Test
    fun `media previous maps to prev`() {
        assertEquals(Action.PREV, translateKey(KeyEvent.KEYCODE_MEDIA_PREVIOUS))
    }

    @Test
    fun `directional keys map correctly`() {
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_DPAD_RIGHT))
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_DPAD_DOWN))
        assertEquals(Action.PREV, translateKey(KeyEvent.KEYCODE_DPAD_LEFT))
        assertEquals(Action.PREV, translateKey(KeyEvent.KEYCODE_DPAD_UP))
    }

    @Test
    fun `unknown key ignored`() {
        assertEquals(Action.IGNORED, translateKey(KeyEvent.KEYCODE_BACK))
    }

    @Test
    fun `page keys map correctly`() {
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_PAGE_DOWN))
        assertEquals(Action.PREV, translateKey(KeyEvent.KEYCODE_PAGE_UP))
    }

    @Test
    fun `camera shutter remote keys map to next`() {
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_CAMERA))
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_FOCUS))
    }

    @Test
    fun `fast forward and rewind map like next prev`() {
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_MEDIA_FAST_FORWARD))
        assertEquals(Action.PREV, translateKey(KeyEvent.KEYCODE_MEDIA_REWIND))
    }

    @Test
    fun `media transport keys map to toggle play`() {
        assertEquals(Action.TOGGLE, translateKey(KeyEvent.KEYCODE_MEDIA_PLAY))
        assertEquals(Action.TOGGLE, translateKey(KeyEvent.KEYCODE_MEDIA_PAUSE))
        assertEquals(Action.TOGGLE, translateKey(KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE))
    }

    @Test
    fun `profile switch remote key maps to next page`() {
        assertEquals(Action.NEXT, translateKey(KeyEvent.KEYCODE_PROFILE_SWITCH))
    }

    @Test
    fun `space enter and dpad center map to toggle play`() {
        assertEquals(Action.TOGGLE, translateKey(KeyEvent.KEYCODE_SPACE))
        assertEquals(Action.TOGGLE, translateKey(KeyEvent.KEYCODE_ENTER))
        assertEquals(Action.TOGGLE, translateKey(KeyEvent.KEYCODE_DPAD_CENTER))
    }
}