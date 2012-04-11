package com.handbagdevices.handbag;

import android.content.Context;

public abstract class FeatureConfig {

    abstract void doAction();


    public static FeatureConfig fromArray(Context context, String[] theArray) {
        // This is a work around for Java not having static methods in Interfaces
        // and not having overridable static methods in Abstract Classes.
        // All subclasses of this class should "hide" (because you can't override)
        // this static method.
        // The context (of the parse service) is supplied for those features that need it.
        throw new UnsupportedOperationException();
    }

}
