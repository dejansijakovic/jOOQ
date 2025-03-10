/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * Other licenses:
 * -----------------------------------------------------------------------------
 * Commercial licenses for this work are available. These replace the above
 * ASL 2.0 and offer limited warranties, support, maintenance, and commercial
 * database integrations.
 *
 * For more information, please visit: https://www.jooq.org/legal/licensing
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 *
 */
package org.jooq.impl;

import static java.util.Collections.unmodifiableCollection;
// ...
// ...
import static org.jooq.SQLDialect.FIREBIRD;
import static org.jooq.SQLDialect.H2;
import static org.jooq.SQLDialect.HSQLDB;
// ...
import static org.jooq.SQLDialect.MARIADB;
import static org.jooq.SQLDialect.MYSQL;
import static org.jooq.SQLDialect.POSTGRES;
import static org.jooq.SQLDialect.SQLITE;
import static org.jooq.SQLDialect.TRINO;
import static org.jooq.SQLDialect.YUGABYTEDB;
import static org.jooq.impl.CommentImpl.NO_COMMENT;
import static org.jooq.impl.DSL.systemName;
import static org.jooq.impl.DefaultBinding.binding;
import static org.jooq.impl.SQLDataType.BIGINT;
import static org.jooq.impl.SQLDataType.BINARY;
import static org.jooq.impl.SQLDataType.BIT;
import static org.jooq.impl.SQLDataType.BLOB;
import static org.jooq.impl.SQLDataType.BOOLEAN;
import static org.jooq.impl.SQLDataType.CHAR;
import static org.jooq.impl.SQLDataType.CLOB;
import static org.jooq.impl.SQLDataType.DATE;
import static org.jooq.impl.SQLDataType.DECIMAL;
import static org.jooq.impl.SQLDataType.DOUBLE;
import static org.jooq.impl.SQLDataType.FLOAT;
import static org.jooq.impl.SQLDataType.INTEGER;
import static org.jooq.impl.SQLDataType.LONGNVARCHAR;
import static org.jooq.impl.SQLDataType.LONGVARBINARY;
import static org.jooq.impl.SQLDataType.LONGVARCHAR;
import static org.jooq.impl.SQLDataType.NCHAR;
import static org.jooq.impl.SQLDataType.NCLOB;
import static org.jooq.impl.SQLDataType.NUMERIC;
import static org.jooq.impl.SQLDataType.NVARCHAR;
import static org.jooq.impl.SQLDataType.OTHER;
import static org.jooq.impl.SQLDataType.REAL;
import static org.jooq.impl.SQLDataType.RECORD;
import static org.jooq.impl.SQLDataType.RESULT;
import static org.jooq.impl.SQLDataType.SMALLINT;
import static org.jooq.impl.SQLDataType.TIME;
import static org.jooq.impl.SQLDataType.TIMESTAMP;
import static org.jooq.impl.SQLDataType.TIMESTAMPWITHTIMEZONE;
import static org.jooq.impl.SQLDataType.TIMEWITHTIMEZONE;
import static org.jooq.impl.SQLDataType.TINYINT;
import static org.jooq.impl.SQLDataType.VARBINARY;
import static org.jooq.impl.SQLDataType.VARCHAR;
import static org.jooq.impl.SQLDataType.XML;
import static org.jooq.impl.Tools.getRecordQualifier;
import static org.jooq.tools.reflect.Reflect.wrapper;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.sql.SQLType;
import java.sql.Types;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;
import java.util.regex.Pattern;

// ...
import org.jooq.Binding;
import org.jooq.CharacterSet;
import org.jooq.Collation;
import org.jooq.Configuration;
import org.jooq.Converter;
import org.jooq.DataType;
import org.jooq.EnumType;
import org.jooq.Field;
import org.jooq.Generator;
import org.jooq.Name;
import org.jooq.Nullability;
import org.jooq.QualifiedRecord;
import org.jooq.SQLDialect;
import org.jooq.exception.MappingException;
import org.jooq.exception.SQLDialectNotSupportedException;
import org.jooq.impl.DefaultBinding.InternalBinding;
import org.jooq.impl.QOM.GenerationLocation;
import org.jooq.impl.QOM.GenerationOption;
import org.jooq.tools.JooqLogger;
import org.jooq.types.UByte;
import org.jooq.types.UInteger;
import org.jooq.types.ULong;
import org.jooq.types.UShort;

import org.jetbrains.annotations.ApiStatus.Internal;

/**
 * A common base class for data types.
 * <p>
 * This also acts as a static data type registry for jOOQ internally.
 * <p>
 * This type is for JOOQ INTERNAL USE only. Do not reference directly
 *
 * @author Lukas Eder
 */
@SuppressWarnings({"unchecked"})
@Internal
public class DefaultDataType<T> extends AbstractDataTypeX<T> {

    private static final Set<SQLDialect>                        ENCODED_TIMESTAMP_PRECISION     = SQLDialect.supportedBy(HSQLDB, MARIADB);
    private static final Set<SQLDialect>                        NO_SUPPORT_TIMESTAMP_PRECISION  = SQLDialect.supportedBy(FIREBIRD, MYSQL, SQLITE);
    private static final Set<SQLDialect>                        SUPPORT_POSTGRES_ARRAY_NOTATION = SQLDialect.supportedBy(POSTGRES, YUGABYTEDB);
    private static final Set<SQLDialect>                        SUPPORT_HSQLDB_ARRAY_NOTATION   = SQLDialect.supportedBy(H2, HSQLDB);
    private static final Set<SQLDialect>                        SUPPORT_TRINO_ARRAY_NOTATION    = SQLDialect.supportedBy(TRINO);

    /**
     * A pattern for data type name normalisation.
     */
    private static final Pattern                                NORMALISE_PATTERN = Pattern.compile("\"|\\.|\\s|\\(\\w+(\\s*,\\s*\\w+)*\\)|(NOT\\s*NULL)?");

    /**
     * A pattern to be used to replace all precision, scale, and length
     * information.
     */
    private static final Pattern                                TYPE_NAME_PATTERN = Pattern.compile("\\([^)]*\\)");

    // -------------------------------------------------------------------------
    // Data type caches
    // -------------------------------------------------------------------------

    /**
     * A cache for dialect-specific data types by normalised.
     */
    private static final Map<String, DefaultDataType<?>>[]      TYPES_BY_NAME;

    /**
     * A cache for dialect-specific data types by Java type.
     */
    private static final Map<Class<?>, DefaultDataType<?>>[]    TYPES_BY_TYPE;

    /**
     * A cache for dialect-specific data types by SQL DataTypes.
     */
    private static final Map<DataType<?>, DefaultDataType<?>>[] TYPES_BY_SQL_DATATYPE;

    /**
     * A cache for SQL DataTypes by Java type.
     */
    private static final Map<Class<?>, DefaultDataType<?>>      SQL_DATATYPES_BY_TYPE;

    // -------------------------------------------------------------------------
    // Precisions
    // -------------------------------------------------------------------------

    /**
     * The minimum decimal precision needed to represent a Java {@link Long}
     * type.
     */
    static final int                                            LONG_PRECISION    = String.valueOf(Long.MAX_VALUE).length();

    /**
     * The minimum decimal precision needed to represent a Java {@link Integer}
     * type.
     */
    static final int                                            INTEGER_PRECISION = String.valueOf(Integer.MAX_VALUE).length();

    /**
     * The minimum decimal precision needed to represent a Java {@link Short}
     * type.
     */
    static final int                                            SHORT_PRECISION   = String.valueOf(Short.MAX_VALUE).length();

    /**
     * The minimum decimal precision needed to represent a Java {@link Byte}
     * type.
     */
    static final int                                            BYTE_PRECISION    = String.valueOf(Byte.MAX_VALUE).length();

    // -------------------------------------------------------------------------
    // Data type attributes
    // -------------------------------------------------------------------------

    /**
     * The SQL dialect associated with this data type.
     */
    private final SQLDialect                                    dialect;

    /**
     * The SQL DataType corresponding to this data type.
     */
    private final DataType<T>                                   sqlDataType;

    /**
     * The Java class corresponding to this data type's <code>&lt;U&gt;</code>
     * type, i.e. the user type in case a {@link Binding} applies.
     */
    private final Class<T>                                      uType;

    /**
     * The Java class corresponding to this data type's <code>&lt;T&gt;</code>
     * type, i.e. the database type in case a {@link Binding} applies.
     */
    private final Class<?>                                      tType;

    /**
     * The data type binding corresponding to this data type.
     */
    private final Binding<?, T>                                 binding;

    /**
     * The type name used for casting to this type.
     */
    private final String                                        castTypeName;

    /**
     * The type name prefix (prior to length, precision, scale) used for casting
     * to this type.
     */
    private final String                                        castTypePrefix;

    /**
     * The type name suffix (after length, precision, scale) used for casting to
     * this type.
     */
    private final String                                        castTypeSuffix;

    /**
     * The type name.
     */
    private final String                                        typeName;

    private final Nullability                                   nullability;
    private final boolean                                       readonly;
    private final Generator<?, ?, T>                            generatedAlwaysAs;
    private final GenerationOption                              generationOption;
    private final GenerationLocation                            generationLocation;
    private final Collation                                     collation;
    private final CharacterSet                                  characterSet;
    private final boolean                                       identity;
    private final Field<T>                                      defaultValue;
    private final Integer                                       precision;
    private final Integer                                       scale;
    private final Integer                                       length;

    static {
        TYPES_BY_SQL_DATATYPE = new Map[SQLDialect.values().length];
        TYPES_BY_NAME = new Map[SQLDialect.values().length];
        TYPES_BY_TYPE = new Map[SQLDialect.values().length];

        for (SQLDialect dialect : SQLDialect.values()) {
            TYPES_BY_SQL_DATATYPE[dialect.ordinal()] = new LinkedHashMap<>();
            TYPES_BY_NAME[dialect.ordinal()] = new LinkedHashMap<>();
            TYPES_BY_TYPE[dialect.ordinal()] = new LinkedHashMap<>();
        }

        SQL_DATATYPES_BY_TYPE = new LinkedHashMap<>();

        // [#2506] Transitively load all dialect-specific data types
        try {
            Class.forName(SQLDataType.class.getName());
        }
        catch (Exception ignore) {}
    }

    public DefaultDataType(SQLDialect dialect, DataType<T> sqlDataType, String typeName) {
        this(dialect, sqlDataType, typeName, null);
    }

    public DefaultDataType(SQLDialect dialect, DataType<T> sqlDataType, String typeName, String castTypeName) {
        this(
            dialect,
            sqlDataType,
            sqlDataType.getType(),
            sqlDataType.getQualifiedName(),
            typeName,
            castTypeName,
            sqlDataType.precisionDefined() ? sqlDataType.precision() : null,
            sqlDataType.scaleDefined() ? sqlDataType.scale() : null,
            sqlDataType.lengthDefined() ? sqlDataType.length() : null,
            sqlDataType.nullability(),
            sqlDataType.defaultValue()
        );
    }

    public DefaultDataType(SQLDialect dialect, Class<T> type, String typeName) {
        this(dialect, null, type, systemName(typeName), typeName, null, null, null, null, Nullability.DEFAULT, null);
    }

    public DefaultDataType(SQLDialect dialect, Class<T> type, String typeName, String castTypeName) {
        this(dialect, null, type, systemName(typeName), typeName, castTypeName, null, null, null, Nullability.DEFAULT, null);
    }

    DefaultDataType(SQLDialect dialect, Class<T> type, Name qualifiedTypeName) {
        this(dialect, null, type, qualifiedTypeName, null, null, null, null, null, Nullability.DEFAULT, null);
    }

    DefaultDataType(SQLDialect dialect, DataType<T> type, Name qualifiedTypeName) {
        this(dialect, type, type.getType(), qualifiedTypeName, null, null, null, null, null, Nullability.DEFAULT, null);
    }

    DefaultDataType(SQLDialect dialect, Class<T> type, Binding<?, T> binding, Name qualifiedTypeName, String typeName, String castTypeName, Integer precision, Integer scale, Integer length, Nullability nullability, Field<T> defaultValue) {
        this(dialect, null, type, binding, qualifiedTypeName, typeName, castTypeName, precision, scale, length, nullability, defaultValue);
    }

    DefaultDataType(SQLDialect dialect, DataType<T> sqlDataType, Class<T> type, Name qualifiedTypeName, String typeName, String castTypeName, Integer precision, Integer scale, Integer length, Nullability nullability, Field<T> defaultValue) {
        this(dialect, sqlDataType, type, null, qualifiedTypeName, typeName, castTypeName, precision, scale, length, nullability, defaultValue);
    }

    DefaultDataType(SQLDialect dialect, DataType<T> sqlDataType, Class<T> type, Binding<?, T> binding, Name qualifiedTypeName, String typeName, String castTypeName, Integer precision, Integer scale, Integer length, Nullability nullability, Field<T> defaultValue) {
        this(dialect, sqlDataType, type, binding, qualifiedTypeName, typeName, castTypeName, precision, scale, length, nullability, false, null, GenerationOption.DEFAULT, GenerationLocation.SERVER, null, null, false, defaultValue);
    }

    DefaultDataType(SQLDialect dialect, DataType<T> sqlDataType, Class<T> type, Binding<?, T> binding, Name qualifiedTypeName, String typeName, String castTypeName, Integer precision, Integer scale, Integer length, Nullability nullability, boolean readonly, Generator<?, ?, T> generatedAlwaysAs, GenerationOption generationOption, GenerationLocation generationLocation, Collation collation, CharacterSet characterSet, boolean identity, Field<T> defaultValue) {
        super(qualifiedTypeName, NO_COMMENT);

        // [#13934] Patch parameters
        if (typeName == null)
            typeName = qualifiedTypeName.toString();

        // Initialise final instance members
        // ---------------------------------

        this.dialect = dialect;

        // [#858] [#11086] SQLDataTypes should reference themselves for more convenience
        this.sqlDataType = (dialect == null && sqlDataType == null) ? this : sqlDataType;
        this.uType = type;
        this.typeName = TYPE_NAME_PATTERN.matcher(typeName).replaceAll("").trim();
        this.castTypeName = castTypeName == null ? this.typeName : castTypeName;

        String[] split = TYPE_NAME_PATTERN.split(castTypeName == null ? typeName : castTypeName);
        this.castTypePrefix = split.length > 0 ? split[0] : "";
        this.castTypeSuffix = split.length > 1 ? split[1] : "";

        this.nullability = nullability == null ? Nullability.DEFAULT : nullability;
        this.readonly = readonly;
        this.generatedAlwaysAs = generatedAlwaysAs;
        this.generationOption = generationOption == null ? GenerationOption.DEFAULT : generationOption;
        this.generationLocation = generationLocation == null ? GenerationLocation.SERVER : generationLocation;
        this.collation = collation;
        this.characterSet = characterSet;
        this.identity = identity;
        this.defaultValue = defaultValue;
        this.precision = integerPrecision(type, precision);
        this.scale = scale;
        this.length = length;

        // Register data type in static caches
        // -----------------------------------

        // Dialect-specific data types
        int ordinal = dialect == null ? SQLDialect.DEFAULT.ordinal() : dialect.family().ordinal();

        // [#3225] Avoid normalisation if not necessary
        if (!TYPES_BY_NAME[ordinal].containsKey(typeName.toUpperCase()))
            TYPES_BY_NAME[ordinal].putIfAbsent(DefaultDataType.normalise(typeName), this);

        TYPES_BY_TYPE[ordinal].putIfAbsent(type, this);
        if (sqlDataType != null)
            TYPES_BY_SQL_DATATYPE[ordinal].putIfAbsent(sqlDataType, this);

        // Global data types
        if (dialect == null)
            SQL_DATATYPES_BY_TYPE.putIfAbsent(type, this);

        this.binding = binding != null ? binding : binding(this);
        this.tType = this.binding.converter().fromType();

        // [#13107] Eagerly register array types of built-in types in type registry
        if (this instanceof BuiltInDataType && !tType.isArray())
            getArrayDataType();
    }

    /**
     * [#7811] Allow for subtypes to override the constructor
     */
    @Override
    DefaultDataType<T> construct(
        Integer newPrecision,
        Integer newScale,
        Integer newLength,
        Nullability newNullability,
        boolean newReadonly,
        Generator<?, ?, T> newGeneratedAlwaysAs,
        GenerationOption newGenerationOption,
        GenerationLocation newGenerationLocation,
        Collation newCollation,
        CharacterSet newCharacterSet,
        boolean newIdentity,
        Field<T> newDefaultValue
    ) {
        return new DefaultDataType<>(
            this,
            newPrecision,
            newScale,
            newLength,
            newNullability,
            newReadonly,
            newGeneratedAlwaysAs,
            newGenerationOption,
            newGenerationLocation,
            newCollation,
            newCharacterSet,
            newIdentity,
            newDefaultValue
        );
    }

    /**
     * [#3225] Performant constructor for creating derived types.
     */
    DefaultDataType(
        AbstractDataType<T> t,
        Integer precision,
        Integer scale,
        Integer length,
        Nullability nullability,
        boolean readonly,
        Generator<?, ?, T> generatedAlwaysAs,
        GenerationOption generationOption,
        GenerationLocation generationLocation,
        Collation collation,
        CharacterSet characterSet,
        boolean identity,
        Field<T> defaultValue
    ) {
        super(t.getQualifiedName(), NO_COMMENT);

        this.dialect = t.getDialect();
        this.sqlDataType = t.getSQLDataType();
        this.uType = t.uType0();
        this.tType = t.tType0();
        this.typeName = t.typeName0();
        this.castTypeName = t.castTypeName0();
        this.castTypePrefix = t.castTypePrefix0();
        this.castTypeSuffix = t.castTypeSuffix0();

        this.nullability = nullability;
        this.readonly = readonly;
        this.generatedAlwaysAs = generatedAlwaysAs;
        this.generationOption = generationOption;
        this.generationLocation = generationLocation;
        this.collation = collation;
        this.characterSet = characterSet;
        this.identity = identity;
        this.defaultValue = defaultValue;
        this.precision = integerPrecision(uType, precision);
        this.scale = scale;
        this.length = length;

        // [#10362] User bindings and/or converters need to be retained
        this.binding =
            t.getBinding() instanceof InternalBinding
          ? binding(this, (Converter<T, T>) t.getBinding().converter())
          : t.getBinding();
    }

    private static final Integer integerPrecision(Class<?> type, Integer precision) {
        if (precision == null)
            if (type == Long.class || type == ULong.class)
                precision = LONG_PRECISION;
            else if (type == Integer.class  || type == UInteger.class)
                precision = INTEGER_PRECISION;
            else if (type == Short.class || type == UShort.class)
                precision = SHORT_PRECISION;
            else if (type == Byte.class || type == UByte.class)
                precision = BYTE_PRECISION;

        return precision;
    }

    @Override
    public final Nullability nullability() {
        return nullability;
    }

    @Override
    public final boolean readonly() {
        return readonly;
    }

    @Override
    public final Generator<?, ?, T> generatedAlwaysAsGenerator() {
        return generatedAlwaysAs == null
            ? null
            : generatedAlwaysAs instanceof DefaultGenerator
            ? generatedAlwaysAs
            : new DefaultGenerator<>(generatedAlwaysAs);
    }

    @Override
    public final GenerationOption generationOption() {
        return generationOption;
    }

    @Override
    public final GenerationLocation generationLocation() {
        return generationLocation;
    }

    @Override
    public final Collation collation() {
        return collation;
    }

    @Override
    public final CharacterSet characterSet() {
        return characterSet;
    }

    @Override
    public final boolean identity() {
        return identity;
    }

    @Override
    public final Field<T> default_() {
        return defaultValue;
    }

    @Override
    final Integer precision0() {
        return precision;
    }

    @Override
    final Integer scale0() {
        return scale;
    }

    @Override
    final Integer length0() {
        return length;
    }

    @Override
    public final DataType<T> getSQLDataType() {
        return sqlDataType;
    }

    @Override
    public final DataType<T> getDataType(Configuration configuration) {

        // If this is a SQLDataType find the most suited dialect-specific
        // data type
        if (getDialect() == null) {
            DefaultDataType<T> dataType = (DefaultDataType<T>) TYPES_BY_SQL_DATATYPE[configuration.family().ordinal()]

                // Be sure to reset length, precision, and scale, as those
                // values were not registered in the below cache
                .get(length0(null).precision0((Integer) null, null));

            if (dataType != null)

                // ... and then, set them back to the original value
                // [#2710] TODO: Remove this logic along with cached data types
                return dataType.construct(precision, scale, length, nullability, readonly, generatedAlwaysAs, generationOption, generationLocation, collation, characterSet, identity, defaultValue);
        }

        // If this is already the dialect's specific data type, return this
        else if (getDialect().family() == configuration.family()) {
            return this;
        }

        // If the SQL data type is not available stick with this data type
        else if (getSQLDataType() == null) {
            return this;
        }

        // If this is another dialect's specific data type, recurse
        else {
            return getSQLDataType().getDataType(configuration);
        }

        return this;
    }

    @Override
    public final Class<T> getType() {
        return uType;
    }

    @Override
    public final Binding<?, T> getBinding() {
        return binding;
    }

    @Override
    final String typeName0() {
        return typeName;
    }

    @Override
    final String castTypePrefix0() {
        return castTypePrefix;
    }

    @Override
    final String castTypeSuffix0() {
        return castTypeSuffix;
    }

    @Override
    final String castTypeName0() {
        return castTypeName;
    }

    @Override
    final Class<?> tType0() {
        return tType;
    }

    @Override
    final Class<T> uType0() {
        return uType;
    }

    @Override
    public final SQLDialect getDialect() {
        return dialect;
    }

    public static final DataType<Object> getDefaultDataType(String typeName) {
        return new DefaultDataType<>(SQLDialect.DEFAULT, Object.class, typeName, typeName);
    }

    public static final DataType<Object> getDefaultDataType(SQLDialect dialect, String typeName) {
        return new DefaultDataType<>(dialect, Object.class, typeName, typeName);
    }

    public static final DataType<?> getDataType(SQLDialect dialect, String typeName) {
        if (dialect == null)
            dialect = SQLDialect.DEFAULT;

        SQLDialect family = dialect.family();
        int ordinal = family.ordinal();
        String upper = typeName.toUpperCase();
        String normalised = typeName;
        DataType<?> result = TYPES_BY_NAME[ordinal].get(upper);

        // [#3225] Normalise only if necessary
        if (result == null) {
            result = TYPES_BY_NAME[ordinal].get(normalised = DefaultDataType.normalise(typeName));

            // UDT data types and others are registered using DEFAULT
            if (result == null) {
                result = TYPES_BY_NAME[SQLDialect.DEFAULT.ordinal()].get(normalised);

                // [#9797] INT = INTEGER alias in case dialect specific information is not available
                // [#5713] TODO: A more generic type aliasing system would be useful, in general!
                if (result == null && "INT".equals(normalised))
                    result = TYPES_BY_NAME[SQLDialect.DEFAULT.ordinal()].get("INTEGER");

                // [#4065] PostgreSQL reports array types as _typename, e.g. _varchar
                else if (result == null && SUPPORT_POSTGRES_ARRAY_NOTATION.contains(dialect) && normalised.charAt(0) == '_')
                    result = getDataType(dialect, normalised.substring(1)).getArrayDataType();

                // [#6466] HSQLDB reports array types as XYZARRAY. H2 should, too
                else if (result == null && SUPPORT_HSQLDB_ARRAY_NOTATION.contains(dialect) && upper.endsWith(" ARRAY"))
                    result = getDataType(dialect, typeName.substring(0, typeName.length() - 6)).getArrayDataType();

                // [#9609] H2 might still report an untyped array, too
                else if (result == null && SUPPORT_HSQLDB_ARRAY_NOTATION.contains(dialect) && upper.equals("ARRAY"))
                    result = SQLDataType.OTHER.getArrayDataType();

                // [#11485] Trino lists arrays as array(component_type)
                else if (result == null && SUPPORT_TRINO_ARRAY_NOTATION.contains(dialect) && upper.startsWith("ARRAY("))
                    result = getDataType(dialect, typeName.substring(6, typeName.length() - 1)).getArrayDataType();








                // [#366] Don't log a warning here. The warning is logged when
                // catching the exception in jOOQ-codegen
                if (result == null)
                    throw new SQLDialectNotSupportedException("Type " + typeName + " is not supported in dialect " + dialect, false);
            }
        }

        return result;
    }

    public static final DataType<?> getDataType(SQLDialect dialect, SQLType sqlType) {
        Integer i = sqlType.getVendorTypeNumber();
        return i == null ? OTHER : getDataType(dialect, i);
    }

    public static final DataType<?> getDataType(SQLDialect dialect, int sqlType) {
        switch (sqlType) {
            case Types.BIGINT:
                return BIGINT;
            case Types.BINARY:
                return BINARY;
            case Types.BIT:
                return BIT;
            case Types.BLOB:
                return BLOB;
            case Types.BOOLEAN:
                return BOOLEAN;
            case Types.CHAR:
                return CHAR;
            case Types.CLOB:
                return CLOB;
            case Types.DATE:
                return DATE;
            case Types.DECIMAL:
                return DECIMAL;
            case Types.DOUBLE:
                return DOUBLE;
            case Types.FLOAT:
                return FLOAT;
            case Types.INTEGER:
                return INTEGER;
            case Types.LONGNVARCHAR:
                return LONGNVARCHAR;
            case Types.LONGVARBINARY:
                return LONGVARBINARY;
            case Types.LONGVARCHAR:
                return LONGVARCHAR;
            case Types.NCHAR:
                return NCHAR;
            case Types.NCLOB:
                return NCLOB;
            case Types.NUMERIC:
                return NUMERIC;
            case Types.NVARCHAR:
                return NVARCHAR;
            case Types.REAL:
                return REAL;
            case Types.REF_CURSOR:
                return RESULT;
            case Types.SMALLINT:
                return SMALLINT;
            case Types.SQLXML:
                return XML;
            case Types.STRUCT:
                return RECORD;
            case Types.TIME:
                return TIME;
            case Types.TIME_WITH_TIMEZONE:
                return TIMEWITHTIMEZONE;
            case Types.TIMESTAMP:
                return TIMESTAMP;
            case Types.TIMESTAMP_WITH_TIMEZONE:
                return TIMESTAMPWITHTIMEZONE;
            case Types.TINYINT:
                return TINYINT;
            case Types.VARBINARY:
                return VARBINARY;
            case Types.VARCHAR:
                return VARCHAR;
            default:
                return OTHER;
        }
    }

    public static final <T> DataType<T> getDataType(SQLDialect dialect, Class<T> type) {
        return getDataType(dialect, type, null);
    }

    private static final JooqLogger getDataType = JooqLogger.getLogger(DefaultDataType.class, "getDataType", 5);
    private static final class DiscouragedStaticTypeRegistryUsage extends RuntimeException {}

    public static final <T> DataType<T> getDataType(SQLDialect dialect, Class<T> type, DataType<T> fallbackDataType) {
        return check(getDataType0(dialect, type, fallbackDataType));
    }

    private static final <T> DataType<T> check(DataType<T> result) {

        // [#5713] [#15286] TODO: Move this to a dynamic type registry and make warning configurable
        if (result instanceof ConvertedDataType || result instanceof LegacyConvertedDataType)
            getDataType.warn("Static type registry", "The deprecated static type registry was being accessed for a non-built-in data type: " + result + ". It is strongly recommended not looking up DataType<T> references from Class<T> references by relying on the internal static type registry. See https://github.com/jOOQ/jOOQ/issues/15286 for details.", new DiscouragedStaticTypeRegistryUsage());

        if (result instanceof ArrayDataType<?> a)
            check(a.elementType);

        return result;
    }

    private static final <T> DataType<T> getDataType0(SQLDialect dialect, Class<T> type, DataType<T> fallbackDataType) {

        // Treat primitive types the same way as their respective wrapper types
        type = wrapper(type);

        // Recurse for arrays
        if (byte[].class != type && type.isArray()) {
            return (DataType<T>) getDataType(dialect, type.getComponentType()).getArrayDataType();
        }

        // Base types are registered statically
        else {
            DataType<?> result = null;

            if (dialect != null)
                result = TYPES_BY_TYPE[dialect.family().ordinal()].get(type);

            if (result == null) {

                // jOOQ data types are handled here
                try {

                    // [#7174] PostgreSQL table records can be function argument types
                    if (QualifiedRecord.class.isAssignableFrom(type))
                        return (DataType<T>) getRecordQualifier(type).getDataType();






                    else if (EnumType.class.isAssignableFrom(type))
                        return (DataType<T>) SQLDataType.VARCHAR.asEnumDataType((Class<EnumType>) type);
                }
                catch (Exception e) {
                    throw new MappingException("Cannot create instance of " + type, e);
                }
            }

            if (result == null) {
                if (SQL_DATATYPES_BY_TYPE.get(type) != null)
                    return (DataType<T>) SQL_DATATYPES_BY_TYPE.get(type);

                // If we have a "fallback" data type from an outer context
                else if (fallbackDataType != null)
                    return fallbackDataType;

                // [#8022] Special handling
                else if (java.util.Date.class == type)
                    return (DataType<T>) SQLDataType.TIMESTAMP;

                // All other data types are illegal
                else
                    throw new SQLDialectNotSupportedException("Type " + type + " is not supported in dialect " + dialect);
            }

            return (DataType<T>) result;
        }
    }

    /**
     * @return The type name without all special characters and white spaces
     */
    public static final String normalise(String typeName) {
        return NORMALISE_PATTERN.matcher(typeName.toUpperCase()).replaceAll("");
    }

    /**
     * Convert a type name (using precision and scale) into a Java class
     */
    public static final DataType<?> getDataType(SQLDialect dialect, String t, int p, int s) throws SQLDialectNotSupportedException {
        return getDataType(dialect, t, p, s, true);
    }

    /**
     * Convert a type name (using precision and scale) into a Java class
     */
    public static final DataType<?> getDataType(SQLDialect dialect, String t, int p, int s, boolean forceIntegerTypesOnZeroScaleDecimals) throws SQLDialectNotSupportedException {
        DataType<?> result = DefaultDataType.getDataType(dialect, t);
        boolean array = result.isArray();

        // [#11307] Apply length, precision, scale on the component type, not the array type
        if (array)
            result = result.getArrayComponentDataType();

        if (forceIntegerTypesOnZeroScaleDecimals && result.getType() == BigDecimal.class)
            result = DefaultDataType.getDataType(dialect, getNumericClass(p, s));

        // [#10809] Use dialect only for lookup, don't report the dialect-specific type
        if (result.getSQLDataType() != null)
            result = result.getSQLDataType();

        if (result.hasPrecision() && result.hasScale())
            result = result.precision(p, s);

        // [#9590] Timestamp precision is in the scale column in some dialects
        else if (result.hasPrecision() && result.isDateTime()) {
            if (ENCODED_TIMESTAMP_PRECISION.contains(dialect))
                result = result.precision(decodeTimestampPrecision(p));
            else if (!NO_SUPPORT_TIMESTAMP_PRECISION.contains(dialect))
                result = result.precision(s);
        }
        else if (result.hasPrecision())
            result = result.precision(p);

        else if (result.hasLength())
            result = result.length(p);

        if (array)
            result = result.getArrayDataType();

        return result;
    }

    private static final int decodeTimestampPrecision(int precision) {

        // [#9590] Discovered empirically from COLUMN_SIZE
        return Math.max(0, precision - 20);
    }

    /**
     * Convert a type name (using precision and scale) into a Java class
     */
    public static final Class<?> getType(SQLDialect dialect, String t, int p, int s) throws SQLDialectNotSupportedException {
        return getDataType(dialect, t, p, s).getType();
    }

    /**
     * Get the most suitable Java class for a given precision and scale
     */
    private static final Class<?> getNumericClass(int precision, int scale) {

        // Integer numbers
        if (scale == 0 && precision != 0)
            if (precision < BYTE_PRECISION)
                return Byte.class;
            else if (precision < SHORT_PRECISION)
                return Short.class;
            else if (precision < INTEGER_PRECISION)
                return Integer.class;
            else if (precision < LONG_PRECISION)
                return Long.class;

            // Default integer number
            else
                return BigInteger.class;

        // Real numbers should not be represented as float or double
        else
            return BigDecimal.class;
    }

    static final Collection<Class<?>> types() {
        return unmodifiableCollection(SQL_DATATYPES_BY_TYPE.keySet());
    }

    static final Collection<DefaultDataType<?>> dataTypes() {
        return unmodifiableCollection(SQL_DATATYPES_BY_TYPE.values());
    }

    static final DataType<?> set(DataType<?> d, Integer l, Integer p, Integer s) {
        if (l != null)
            d = d.length(l);

        if (p != null)
            if (s != null)
                d = d.precision(p, s);
            else
                d = d.precision(p);

        return d;
    }
}
