package de.viatorus.neo2externalkeyboard;

import android.inputmethodservice.InputMethodService;
import android.util.Log;
import android.view.KeyCharacterMap;
import android.view.KeyEvent;
import android.widget.Toast;

import java.text.MessageFormat;
import java.util.HashMap;
import java.util.Map;


public class Neo2InputMethodService extends InputMethodService {
    private static final String TAG = "Neo2InputMethodService";

    private static final int Mod2_1 = KeyEvent.KEYCODE_SHIFT_LEFT;
    private static final int Mod2_2 = KeyEvent.KEYCODE_SHIFT_RIGHT;
    private static final int Mod3_1 = KeyEvent.KEYCODE_CAPS_LOCK;
    private static final int Mod3_2 = KeyEvent.KEYCODE_BACKSLASH;
    private static final int Mod4_1 = KeyEvent.KEYCODE_SLASH;
    private static final int Mod4_2 = KeyEvent.KEYCODE_ALT_RIGHT;

    private static final Map<Integer, Integer> textActions = new HashMap<Integer, Integer>() {{
        // Row 1
        put(KeyEvent.KEYCODE_8, KeyEvent.KEYCODE_TAB);
        // Row 2
        put(KeyEvent.KEYCODE_X, KeyEvent.KEYCODE_PAGE_UP);
        put(KeyEvent.KEYCODE_V, KeyEvent.KEYCODE_DEL);
        put(KeyEvent.KEYCODE_L, KeyEvent.KEYCODE_DPAD_UP);
        put(KeyEvent.KEYCODE_C, KeyEvent.KEYCODE_FORWARD_DEL);
        put(KeyEvent.KEYCODE_W, KeyEvent.KEYCODE_PAGE_DOWN);
        // Row 3
        put(KeyEvent.KEYCODE_U, KeyEvent.KEYCODE_MOVE_HOME);
        put(KeyEvent.KEYCODE_I, KeyEvent.KEYCODE_DPAD_LEFT);
        put(KeyEvent.KEYCODE_A, KeyEvent.KEYCODE_DPAD_DOWN);
        put(KeyEvent.KEYCODE_E, KeyEvent.KEYCODE_DPAD_RIGHT);
        put(KeyEvent.KEYCODE_O, KeyEvent.KEYCODE_MOVE_END);
        // Row 4
        put(KeyEvent.KEYCODE_EQUALS, KeyEvent.KEYCODE_ESCAPE);
        put(KeyEvent.KEYCODE_SEMICOLON, KeyEvent.KEYCODE_TAB);
        put(KeyEvent.KEYCODE_APOSTROPHE, KeyEvent.KEYCODE_INSERT);
        put(KeyEvent.KEYCODE_P, KeyEvent.KEYCODE_ENTER);
        put(KeyEvent.KEYCODE_Z, KeyEvent.KEYCODE_BACK);
    }};

    private int mod2_only = 0;
    private boolean mod2_locked = false;
    private boolean mod2_1 = false;
    private boolean mod2_2 = false;
    private boolean mod3_1 = false;
    private boolean mod3_2 = false;
    private int mod4_only = 0;
    private boolean mod4_locked = false;
    private boolean mod4_1 = false;
    private boolean mod4_2 = false;

    /**
     * Adjusts the meta state, according to the state of Mod2-4, of a key event.
     */
    private KeyEvent adjustMetaState(final KeyEvent src) {
        int state = src.getMetaState() & ~(KeyEvent.META_CAPS_LOCK_ON);

        // Keep left alt as valid modifier.
        if ((state & KeyEvent.META_ALT_LEFT_ON) != 0) {
            state &= ~KeyEvent.META_ALT_RIGHT_ON;
        } else {
            state &= ~KeyEvent.META_ALT_MASK;
        }

        // Invert shift if locked.
        if (mod2_locked && (state & (KeyEvent.META_META_MASK | KeyEvent.META_CTRL_MASK)) == 0) {
            if (mod2_1) {
                state ^= KeyEvent.META_SHIFT_LEFT_ON;
            }
            if (mod2_2) {
                state ^= KeyEvent.META_SHIFT_RIGHT_ON;
            }
            state ^= KeyEvent.META_SHIFT_ON;
        }
        if ((mod3_1 || mod3_2)) {
            state |= KeyEvent.META_CAPS_LOCK_ON;
        }
        if ((mod4_1 || mod4_2) ^ mod4_locked) {
            state |= KeyEvent.META_ALT_RIGHT_ON;
        }

        return new KeyEvent(src.getDownTime(), src.getEventTime(), src.getAction(), src.getKeyCode(),
                src.getRepeatCount(), state, src.getDeviceId(), src.getScanCode(), src.getFlags(), src.getSource());
    }

    /**
     * Changes the key code of a key event.
     */
    private KeyEvent changeKeyCode(final KeyEvent src, final int keyCode) {
        int state = src.getMetaState();

        // Keep left alt as valid modifier.
        if ((state & KeyEvent.META_ALT_LEFT_ON) != 0) {
            state &= ~(KeyEvent.META_ALT_RIGHT_ON);
        } else {
            state &= ~(KeyEvent.META_ALT_MASK);
        }

        return new KeyEvent(src.getDownTime(), src.getEventTime(), src.getAction(), keyCode,
                src.getRepeatCount(), state, src.getDeviceId(), src.getScanCode(), src.getFlags(), src.getSource());
    }

    /**
     * Checks and tracks Mod2 (shift) key events.
     * If left and right shift are pressed and released at the same time without any other key event
     * interfering, Mod2 is locked/unlocked.
     */
    private boolean checkForMod2(final int keyCode, final boolean down) {
        if (keyCode == Mod2_1) {
            if (!mod2_1 && down) {
                mod2_only++;
            }
            mod2_1 = down;
            if (!down && mod2_2) {
                mod2_only++;
            } else if (!down && mod2_only == 3) {
                mod2_locked = !mod2_locked;
                mod2_only = 0;
            }
            return true;
        } else if (keyCode == Mod2_2) {
            if (!mod2_2 && down) {
                mod2_only++;
            }
            mod2_2 = down;
            if (!down && mod2_1) {
                mod2_only++;
            } else if (!down && mod2_only == 3) {
                mod2_locked = !mod2_locked;
                mod2_only = 0;
            }
            return true;
        }
        mod2_only = 0;
        return false;
    }

    /**
     * Checks and tracks Mod3 and Mod4 key events.
     */
    private boolean checkForMod3or4(final int keyCode, final boolean down) {
        if (keyCode == Mod4_1) {
            if (!mod4_1 && down) {
                mod4_only++;
            }
            mod4_1 = down;
            if (!down && mod4_2) {
                mod4_only++;
            } else if (!down && mod4_only == 3) {
                mod4_locked = !mod4_locked;
                mod4_only = 0;
            }
            return true;
        } else if (keyCode == Mod4_2) {
            if (!mod4_2 && down) {
                mod4_only++;
            }
            mod4_2 = down;
            if (!down && mod4_1) {
                mod4_only++;
            } else if (!down && mod4_only == 3) {
                mod4_locked = !mod4_locked;
                mod4_only = 0;
            }
            return true;
        } else {
            mod4_only = 0;
            if (keyCode == Mod3_1) {
                mod3_1 = down;
                return true;
            } else if (keyCode == Mod3_2) {
                mod3_2 = down;
                return true;
            }
        }
        return false;
    }

    /**
     * Checks if a text action (e.g. move cursor, delete text) should be performed.
     */
    private boolean checkForTextActions(final int keyCode, KeyEvent event) {
        // Only for mod 4.
        if (((mod4_1 || mod4_2) ^ mod4_locked) && !(mod3_1 || mod3_2)) {
            // Try to find a matching text action.
            Integer action = textActions.get(keyCode);
            if (action != null) {
                event = changeKeyCode(event, action);
                if (!super.onKeyDown(action, event)) {
                    getCurrentInputConnection().sendKeyEvent(event);
                }
                return true;
            }
        }
        return false;
    }

    /**
     * Checks and handles the compose key.
     */
    private boolean checkForCompose(final int keyCode, final KeyEvent event, final boolean down) {
        if (keyCode == KeyEvent.KEYCODE_TAB && event.getMetaState() == KeyEvent.META_CAPS_LOCK_ON) {
            Toast.makeText(this, "The compose key is not yet supported.", Toast.LENGTH_SHORT).show();
            return true;
        }
        return false;
    }

    /**
     * Handles all incoming key events.
     */
    private boolean handleKeyEvent(final int keyCode, KeyEvent event, final boolean down) {
        //  Log.d(TAG, MessageFormat.format("Key {0} with meta state {1} {2}.", keyCode, event.getMetaState(), down ? "down" : "up"));

        if (checkForMod2(keyCode, down)) {
            mod4_only = 0;
            // Only for shift key, continue propagation.
            return false;
        }
        if (checkForMod3or4(keyCode, down) || checkForTextActions(keyCode, event)) {
            return true;
        }

        // Adjust meta state manually because, e.g. caps lock can be locked by internal hardware.
        event = adjustMetaState(event);

        if (checkForCompose(keyCode, event, down)) {
            return true;
        }

        // Handle all other keys to work with the key character map.
        getCurrentInputConnection().sendKeyEvent(event);
        return true;
    }

    @Override
    public boolean onKeyDown(final int keyCode, final KeyEvent event) {
        if (handleKeyEvent(keyCode, event, true)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    public boolean onKeyUp(final int keyCode, final KeyEvent event) {
        if (handleKeyEvent(keyCode, event, false)) {
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }
}
