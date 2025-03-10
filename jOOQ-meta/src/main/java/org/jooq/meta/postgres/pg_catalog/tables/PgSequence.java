/*
 * This file is generated by jOOQ.
 */
package org.jooq.meta.postgres.pg_catalog.tables;


import java.util.Arrays;
import java.util.List;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.ForeignKey;
import org.jooq.InverseForeignKey;
import org.jooq.Name;
import org.jooq.Path;
import org.jooq.Record;
import org.jooq.Schema;
import org.jooq.Table;
import org.jooq.TableField;
import org.jooq.TableOptions;
import org.jooq.UniqueKey;
import org.jooq.impl.DSL;
import org.jooq.impl.SQLDataType;
import org.jooq.impl.TableImpl;
import org.jooq.meta.postgres.pg_catalog.Keys;
import org.jooq.meta.postgres.pg_catalog.PgCatalog;
import org.jooq.meta.postgres.pg_catalog.tables.PgClass.PgClassPath;
import org.jooq.meta.postgres.pg_catalog.tables.PgType.PgTypePath;


/**
 * This class is generated by jOOQ.
 */
@SuppressWarnings({ "all", "unchecked", "rawtypes" })
public class PgSequence extends TableImpl<Record> {

    private static final long serialVersionUID = 1L;

    /**
     * The reference instance of <code>pg_catalog.pg_sequence</code>
     */
    public static final PgSequence PG_SEQUENCE = new PgSequence();

    /**
     * The class holding records for this type
     */
    @Override
    public Class<Record> getRecordType() {
        return Record.class;
    }

    /**
     * The column <code>pg_catalog.pg_sequence.seqrelid</code>.
     */
    public final TableField<Record, Long> SEQRELID = createField(DSL.name("seqrelid"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>pg_catalog.pg_sequence.seqtypid</code>.
     */
    public final TableField<Record, Long> SEQTYPID = createField(DSL.name("seqtypid"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>pg_catalog.pg_sequence.seqstart</code>.
     */
    public final TableField<Record, Long> SEQSTART = createField(DSL.name("seqstart"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>pg_catalog.pg_sequence.seqincrement</code>.
     */
    public final TableField<Record, Long> SEQINCREMENT = createField(DSL.name("seqincrement"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>pg_catalog.pg_sequence.seqmax</code>.
     */
    public final TableField<Record, Long> SEQMAX = createField(DSL.name("seqmax"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>pg_catalog.pg_sequence.seqmin</code>.
     */
    public final TableField<Record, Long> SEQMIN = createField(DSL.name("seqmin"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>pg_catalog.pg_sequence.seqcache</code>.
     */
    public final TableField<Record, Long> SEQCACHE = createField(DSL.name("seqcache"), SQLDataType.BIGINT.nullable(false), this, "");

    /**
     * The column <code>pg_catalog.pg_sequence.seqcycle</code>.
     */
    public final TableField<Record, Boolean> SEQCYCLE = createField(DSL.name("seqcycle"), SQLDataType.BOOLEAN.nullable(false), this, "");

    private PgSequence(Name alias, Table<Record> aliased) {
        this(alias, aliased, (Field<?>[]) null, null);
    }

    private PgSequence(Name alias, Table<Record> aliased, Field<?>[] parameters, Condition where) {
        super(alias, null, aliased, parameters, DSL.comment(""), TableOptions.table(), where);
    }

    /**
     * Create an aliased <code>pg_catalog.pg_sequence</code> table reference
     */
    public PgSequence(String alias) {
        this(DSL.name(alias), PG_SEQUENCE);
    }

    /**
     * Create an aliased <code>pg_catalog.pg_sequence</code> table reference
     */
    public PgSequence(Name alias) {
        this(alias, PG_SEQUENCE);
    }

    /**
     * Create a <code>pg_catalog.pg_sequence</code> table reference
     */
    public PgSequence() {
        this(DSL.name("pg_sequence"), null);
    }

    public <O extends Record> PgSequence(Table<O> path, ForeignKey<O, Record> childPath, InverseForeignKey<O, Record> parentPath) {
        super(path, childPath, parentPath, PG_SEQUENCE);
    }

    public static class PgSequencePath extends PgSequence implements Path<Record> {
        public <O extends Record> PgSequencePath(Table<O> path, ForeignKey<O, Record> childPath, InverseForeignKey<O, Record> parentPath) {
            super(path, childPath, parentPath);
        }
    }

    @Override
    public Schema getSchema() {
        return aliased() ? null : PgCatalog.PG_CATALOG;
    }

    @Override
    public UniqueKey<Record> getPrimaryKey() {
        return Keys.PG_SEQUENCE_SEQRELID_INDEX;
    }

    @Override
    public List<ForeignKey<Record, ?>> getReferences() {
        return Arrays.asList(Keys.PG_SEQUENCE__SYNTHETIC_FK_PG_SEQUENCE__SYNTHETIC_PK_PG_CLASS, Keys.PG_SEQUENCE__SYNTHETIC_FK_PG_SEQUENCE__SYNTHETIC_PK_PG_TYPE);
    }

    private transient PgClassPath _pgClass;

    /**
     * Get the implicit join path to the <code>pg_catalog.pg_class</code> table.
     */
    public PgClassPath pgClass() {
        if (_pgClass == null)
            _pgClass = new PgClassPath(this, Keys.PG_SEQUENCE__SYNTHETIC_FK_PG_SEQUENCE__SYNTHETIC_PK_PG_CLASS, null);

        return _pgClass;
    }

    private transient PgTypePath _pgType;

    /**
     * Get the implicit join path to the <code>pg_catalog.pg_type</code> table.
     */
    public PgTypePath pgType() {
        if (_pgType == null)
            _pgType = new PgTypePath(this, Keys.PG_SEQUENCE__SYNTHETIC_FK_PG_SEQUENCE__SYNTHETIC_PK_PG_TYPE, null);

        return _pgType;
    }

    @Override
    public PgSequence as(String alias) {
        return new PgSequence(DSL.name(alias), this);
    }

    @Override
    public PgSequence as(Name alias) {
        return new PgSequence(alias, this);
    }

    @Override
    public PgSequence as(Table<?> alias) {
        return new PgSequence(alias.getQualifiedName(), this);
    }
}
