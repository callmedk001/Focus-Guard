package com.dk.focusguard.fakes

import com.dk.focusguard.services.accessibility.AccessibleNode

/**
 * Fake implementation of AccessibleNode for testing privacy rules on local JVM.
 */
class FakeAccessibleNode(
    override val isPassword: Boolean = false,
    override val isSelected: Boolean = false,
    override val className: CharSequence? = "android.view.ViewGroup",
    override val viewIdResourceName: String? = null,
    override val contentDescription: CharSequence? = null,
    override val text: CharSequence? = null,
    private val children: List<AccessibleNode> = emptyList()
) : AccessibleNode {
    override val childCount: Int get() = children.size
    override fun getChild(index: Int): AccessibleNode? = children.getOrNull(index)
}

