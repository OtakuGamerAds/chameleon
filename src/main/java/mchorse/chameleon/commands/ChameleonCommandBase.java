package mchorse.chameleon.commands;

import mchorse.chameleon.Chameleon;
import mchorse.mclib.commands.McCommandBase;
import mchorse.mclib.commands.utils.L10n;

/* CLASS COPIED FROM BB */
public abstract class ChameleonCommandBase extends McCommandBase {
    @Override
    public L10n getL10n()
    {
        return Chameleon.l10n;
    }
}
