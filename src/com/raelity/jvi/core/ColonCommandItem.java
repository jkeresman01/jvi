package com.raelity.jvi.core;

import com.raelity.jvi.core.ColonCommands.ColonAction;
import java.util.Collections;
import java.util.EnumSet;
import java.util.Set;

/**
 * A registered command is represented by this class.
 * Comparison and equality is based on the command abbreviation.
 */
public final class ColonCommandItem implements Comparable<ColonCommandItem> {

    private final String abbrev;
    private final String name;
    private final Object value;
    private final EnumSet<CcFlag> flags;

    ColonCommandItem(String abbrev)
    {
        this(abbrev, null, null, null);
    }

    ColonCommandItem(String abbrev, String name, Object value,
                     EnumSet<CcFlag> flags)
    {
        this.abbrev = abbrev;
        this.name = name;
        this.value = value;
        if(flags == null)
            flags = EnumSet.noneOf(CcFlag.class);
        this.flags = flags;
    }

    /**
     * @return the flags
     */
    public Set<CcFlag> getFlags()
    {
        return Collections.unmodifiableSet(flags);
    }

    /**
     * @return the abbreviation for the command
     */
    public String getAbbrev()
    {
        return abbrev;
    }

    /**
     * @return the full command name
     */
    public String getName()
    {
        return name;
    }

    public String getDisplayName()
    {
        if(getValue() instanceof ColonAction)
            return ((ColonAction)getValue()).getDisplayName(this);
        else
            return getName();
    }

    /**
     * @return the value.
     */
    public Object getValue()
    {
        return value;
    }

    /**
     * Check if the argument can invoke this command.
     */
    public boolean match(String tryName)
    {
        return tryName.startsWith(this.abbrev) && this.name.startsWith(tryName);
    }

    @Override
    public int compareTo(ColonCommandItem o1)
    {
        return abbrev.compareTo(o1.abbrev);
    }

    @Override
    public boolean equals(Object o1)
    {
        return o1 instanceof ColonCommandItem ? abbrev.equals(o1) : false;
    }

    @Override
    public int hashCode()
    {
        int hash = 3;
        hash = 29 * hash + (this.abbrev != null ? this.abbrev.hashCode() : 0);
        return hash;
    }
}
