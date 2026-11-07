package fi.dy.masa.servux.dataproviders;

import fi.dy.masa.servux.Reference;
import fi.dy.masa.servux.util.i18n.i18nManager;

/**
 * Minimal settings holder for the 1.20.1 port.
 * The full 1.21 settings/config system is not ported yet; only the pieces used by the
 * Litematica paste code are kept here.
 */
public class ServuxConfigProvider
{
    public static final ServuxConfigProvider INSTANCE = new ServuxConfigProvider();
    public static final i18nManager LANG = i18nManager.create(Reference.MOD_ID);

    private boolean debugMode;

    public boolean hasDebugMode()
    {
        return this.debugMode;
    }

    public void setDebugMode(boolean debugMode)
    {
        this.debugMode = debugMode;
    }

    public boolean isEasyPlaceValidatorEnabled()
    {
        return false;
    }
}
