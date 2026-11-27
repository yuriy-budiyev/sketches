/*
 * MIT License
 *
 * Copyright (c) 2026 Yuriy Budiyev
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package com.github.yuriybudiyev.sketches.core.ui.graphics

import android.graphics.LinearGradient
import android.graphics.Shader
import android.os.Build
import androidx.annotation.FloatRange
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.DefaultAlpha
import androidx.compose.ui.graphics.MeshGradientPainter
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.isSpecified
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.platform.InspectorInfo

const val LowTransparencyAlpha: Float = 0.75F
const val MediumTransparencyAlpha: Float = 0.35F
const val HighTransparencyAlpha: Float = 0.15F

@Stable
fun Color.withLowTransparency(): Color =
    copy(alpha = LowTransparencyAlpha)

@Stable
fun Color.withMediumTransparency(): Color =
    copy(alpha = MediumTransparencyAlpha)

@Stable
fun Color.withHighTransparency(): Color =
    copy(alpha = HighTransparencyAlpha)

@Stable
fun Modifier.solidColorBackground(color: Color): Modifier {
    require(color.isSpecified) { "Color should be specified" }
    return this.then(SolidColorBackgroundElement(color))
}

@Stable
fun Modifier.verticalGradientBackground(
    topColor: Color,
    bottomColor: Color,
    @FloatRange(from = 0.0, to = 1.0)
    start: Float = 0F,
    @FloatRange(from = 0.0, to = 1.0)
    end: Float = 1F,
): Modifier {
    require(topColor.isSpecified) { "Top color should be specified" }
    require(bottomColor.isSpecified) { "Bottom color should be specified" }
    require(start >= 0F) { "Start offset should be >= 0 ($start)" }
    require(end <= 1F) { "End offset should be <= 1 ($end)" }
    require(start < end) { "Start offset ($start) should be less than end offset ($end)" }
    return this.then(VerticalGradientBackgroundElement(topColor, bottomColor, start, end))
}

private class SolidColorBackgroundElement(
    private val color: Color,
): ModifierNodeElement<SolidColorBackgroundNode>() {

    override fun create(): SolidColorBackgroundNode =
        SolidColorBackgroundNode(color)

    override fun update(node: SolidColorBackgroundNode) {
        node.update(color)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "solidColorBackground"
        properties["color"] = color
    }

    override fun equals(other: Any?): Boolean =
        when {
            other === this -> true
            other is SolidColorBackgroundElement -> other.color == this.color
            else -> false
        }

    override fun hashCode(): Int =
        color.hashCode()
}

private class SolidColorBackgroundNode(
    private var color: Color,
): Modifier.Node(), DrawModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    override fun ContentDrawScope.draw() {
        drawRect(color)
        drawContent()
    }

    fun update(color: Color) {
        var changed = false
        if (this.color != color) {
            this.color = color
            changed = true
        }
        if (changed) {
            invalidateDraw()
        }
    }
}

private class VerticalGradientBackgroundElement(
    private val topColor: Color,
    private val bottomColor: Color,
    private val start: Float,
    private val end: Float,
): ModifierNodeElement<VerticalGradientBackgroundNode>() {

    override fun create(): VerticalGradientBackgroundNode =
        VerticalGradientBackgroundNode(topColor, bottomColor, start, end)

    override fun update(node: VerticalGradientBackgroundNode) {
        node.update(topColor, bottomColor, start, end)
    }

    override fun InspectorInfo.inspectableProperties() {
        name = "verticalGradientBackground"
        properties["topColor"] = topColor
        properties["bottomColor"] = bottomColor
        properties["start"] = start
        properties["end"] = end
    }

    override fun equals(other: Any?): Boolean =
        when {
            other === this -> true
            other is VerticalGradientBackgroundElement ->
                other.topColor == this.topColor && other.bottomColor == this.bottomColor &&
                    other.start == this.start && other.end == this.end
            else -> false
        }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + topColor.hashCode()
        result = 31 * result + bottomColor.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }
}

private class VerticalGradientBackgroundNode(
    private var topColor: Color,
    private var bottomColor: Color,
    private var start: Float,
    private var end: Float,
): Modifier.Node(), DrawModifierNode {

    override val shouldAutoInvalidate: Boolean = false

    override fun ContentDrawScope.draw() {
        with(painter) {
            draw(size)
        }
        drawContent()
    }

    fun update(
        topColor: Color,
        bottomColor: Color,
        start: Float,
        end: Float,
    ) {
        var changed = false
        if (this.topColor != topColor) {
            this.topColor = topColor
            changed = true
        }
        if (this.bottomColor != bottomColor) {
            this.bottomColor = bottomColor
            changed = true
        }
        if (this.start != start) {
            this.start = start
            changed = true
        }
        if (this.end != end) {
            this.end = end
            changed = true
        }
        if (changed) {
            painter = createPainter()
            invalidateDraw()
        }
    }

    private fun createPainter(): Painter =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            createMeshGradientPainter()
        } else {
            createLinearGradientPainter()
        }

    private fun createMeshGradientPainter(): Painter {
        var rows = 1
        if (start > 0F) {
            rows++
        }
        if (end < 1F) {
            rows++
        }
        return MeshGradientPainter(
            rows = rows,
            columns = 1,
            hasBicubicColor = true,
        ) {
            var row = 0
            setVertex(row, 0, Offset(0F, 0F), topColor)
            setVertex(row, 1, Offset(1F, 0F), topColor)
            row++
            if (start > 0F) {
                setVertex(row, 0, Offset(0F, start), topColor)
                setVertex(row, 1, Offset(1F, start), topColor)
                row++
            }
            if (end < 1F) {
                setVertex(row, 0, Offset(0F, end), bottomColor)
                setVertex(row, 1, Offset(1F, end), bottomColor)
                row++
            }
            setVertex(row, 0, Offset(0F, 1F), bottomColor)
            setVertex(row, 1, Offset(1F, 1F), bottomColor)
        }
    }

    private fun createLinearGradientPainter(): Painter =
        VerticalLinearGradientPainter(topColor, bottomColor, start, end)

    private var painter: Painter = createPainter()
}

private class VerticalLinearGradientPainter(
    topColor: Color,
    bottomColor: Color,
    start: Float,
    end: Float,
): Painter() {

    override val intrinsicSize: Size = Size.Unspecified

    override fun DrawScope.onDraw() {
        drawRect(brush = brush, alpha = alpha, colorFilter = colorFilter)
    }

    override fun applyAlpha(alpha: Float): Boolean {
        this.alpha = alpha
        return true
    }

    override fun applyColorFilter(colorFilter: ColorFilter?): Boolean {
        this.colorFilter = colorFilter
        return true
    }

    private val brush: ShaderBrush = VerticalLinearGradientBrush(topColor, bottomColor, start, end)
    private var alpha: Float = DefaultAlpha
    private var colorFilter: ColorFilter? = null

    override fun equals(other: Any?): Boolean =
        when {
            other === this -> true
            other is VerticalLinearGradientPainter ->
                other.brush == this.brush && other.alpha == this.alpha && other.colorFilter == this.colorFilter
            else -> false
        }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + brush.hashCode()
        result = 31 * result + alpha.hashCode()
        result = 31 * result + colorFilter.hashCode()
        return result
    }

    override fun toString(): String =
        "VerticalLinearGradientPainter(brush: $brush, alpha: $alpha, colorFilter: $colorFilter)"
}

@Immutable
private class VerticalLinearGradientBrush(
    private val topColor: Color,
    private val bottomColor: Color,
    private val start: Float,
    private val end: Float,
): ShaderBrush() {

    override fun createShader(size: Size): Shader =
        LinearGradient(
            0F,
            size.height * start,
            0F,
            size.height * end,
            topColor.toArgb(),
            bottomColor.toArgb(),
            Shader.TileMode.CLAMP,
        )

    override fun equals(other: Any?): Boolean =
        when {
            other === this -> true
            other is VerticalLinearGradientBrush ->
                other.topColor == this.topColor && other.bottomColor == this.bottomColor &&
                    other.start == this.start && other.end == this.end
            else -> false
        }

    override fun hashCode(): Int {
        var result = 17
        result = 31 * result + topColor.hashCode()
        result = 31 * result + bottomColor.hashCode()
        result = 31 * result + start.hashCode()
        result = 31 * result + end.hashCode()
        return result
    }

    override fun toString(): String =
        "VerticalLinearGradientBrush(topColor: $topColor, bottomColor: $bottomColor, start: $start, end: $end)"
}
