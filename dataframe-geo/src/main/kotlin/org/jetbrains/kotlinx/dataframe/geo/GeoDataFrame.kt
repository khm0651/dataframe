package org.jetbrains.kotlinx.dataframe.geo

import org.geotools.api.referencing.crs.CoordinateReferenceSystem
import org.geotools.geometry.jts.JTS
import org.geotools.referencing.CRS
import org.jetbrains.kotlinx.dataframe.DataFrame
import org.jetbrains.kotlinx.dataframe.api.update
import org.jetbrains.kotlinx.dataframe.api.with

/**
 * A data structure representing a geographical DataFrame, combining spatial data with
 * an optional Coordinate Reference System (CRS).
 *
 * @param T The type parameter extending `WithGeometry`, indicating the presence of a geometry column.
 * @property df The underlying `DataFrame` containing geometries.
 * @property crs The coordinate reference system associated with the data, if any.
 */
class GeoDataFrame<T : WithGeometry>(val df: DataFrame<T>, val crs: CoordinateReferenceSystem?) {
    /**
     * Creates a new `GeoDataFrame` with the modified underlying DataFrame.
     *
     * @param block The block defining the transformations to be applied to the DataFrame.
     * @return A new `GeoDataFrame` instance with updated dataframe and the same CRS.
     */
    inline fun modify(block: DataFrame<T>.() -> DataFrame<T>): GeoDataFrame<T> = GeoDataFrame(df.block(), crs)

    /**
     * Transforms the geometries to a specified Coordinate Reference System (CRS).
     *
     * This function reprojects the geometry data from the current CRS to a target CRS.
     * If no target CRS is specified and the `GeoDataFrame` has no CRS, WGS 84 is used by default.
     *
     * @param targetCrs The target CRS for transformation.
     * @return A new `GeoDataFrame` with reprojected geometries and the specified CRS.
     */
    fun applyCrs(targetCrs: CoordinateReferenceSystem): GeoDataFrame<T> {
        if (crs == null) {
            return GeoDataFrame(df, targetCrs)
        }
        if (targetCrs == this.crs) return this
        // Use WGS 84 by default TODO
        val sourceCRS: CoordinateReferenceSystem = this.crs
        val transform = CRS.findMathTransform(sourceCRS, targetCrs, true)
        return GeoDataFrame(
            df.update { geometry }.with { JTS.transform(it, transform) },
            targetCrs,
        )
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as GeoDataFrame<*>

        if (df != other.df) return false

        return when {
            crs == null && other.crs == null -> true
            crs == null || other.crs == null -> false
            else -> CRS.equalsIgnoreMetadata(crs, other.crs)
        }
    }

    override fun hashCode(): Int {
        var result = df.hashCode()
        result = 31 * result + (crs?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "GeoDataFrame(df=$df, crs=$crs)"

    companion object {
        val DEFAULT_CRS = CRS.decode("EPSG:4326", true)
    }
}
