package com.rainbow.compose.ui.theme

import androidx.compose.ui.graphics.Color

// ════════════════════════════════════════════════════════════════
// 颜色定义
//
// 命名与业务侧 XML colors.xml 完全一致（连字符 → 驼峰）。
// ════════════════════════════════════════════════════════════════

// ── Primary ────────────────────────────────────────────────────
val DramaPrimaryMain      = Color(0xFFFF175D)  // main
val DramaPrimarySecondary = Color(0xFFFF6D99)  // light
val DramaPrimaryWhite     = Color(0xFFFFFFFF)
val DramaPrimaryBlack     = Color(0xFF000000)
val DramaPrimaryAlpha     = Color(0x14FF175D)  // opacity_14

// ── Secondary ──────────────────────────────────────────────────
val DramaSecondary01 = Color(0xFFF54E96)
val DramaSecondary02 = Color(0xFF8773DF)
val DramaSecondary03 = Color(0xFFFFC0D3)

// ── Status ─────────────────────────────────────────────────────
val DramaStatusWarning   = Color(0xFFFA8D19)
val DramaStatusWarning3  = Color(0xFFFF3058)
val DramaStatusSuccess   = Color(0xFF59D66C)
val DramaStatusError     = Color(0xFFEE4747)

// ── Text（Light）───────────────────────────────────────────────
val DramaTextPrimary   = Color(0xFF0C1014)
val DramaTextSecondary = Color(0xFF6A717A)
val DramaTextDisabled  = Color(0xFFBBBDBF)
val DramaTextTertiary  = Color(0xFF9A9EA3)
val DramaTextIcon      = Color(0xFF6A717A)
val DramaTitleBarIcon  = Color(0xFF111111)

// ── Text（Dark）────────────────────────────────────────────────
val DramaTextPrimaryDark   = Color(0xFFFFFFFF)
val DramaTextSecondaryDark = Color(0xFF999999)
val DramaTextDisabledDark  = Color(0xFF666666)
val DramaTextTertiaryDark  = Color(0xFF777777)
val DramaTextIconDark      = Color(0xFFDDDDDD)
val DramaTitleBarIconDark  = Color(0xFFFFFFFF)

// ── New Text（Light）───────────────────────────────────────────
val DramaNewTextPrimary    = Color(0xFF161823)
val DramaNewTextSecondary  = Color(0xBF161823)
val DramaNewTextTertiary   = Color(0x80161823)
val DramaNewTextQuaternary = Color(0x66161823)

// ── New Text（Dark）────────────────────────────────────────────
val DramaNewTextPrimaryDark    = Color(0xFFFFFFFF)
val DramaNewTextSecondaryDark  = Color(0xBFFFFFFF)
val DramaNewTextTertiaryDark   = Color(0x80FFFFFF)
val DramaNewTextQuaternaryDark = Color(0x66FFFFFF)

// ── Background（Light）─────────────────────────────────────────
val DramaBackgroundDefault    = Color(0xFFFFFFFF)
val DramaBackgroundSecondary  = Color(0xFFF3F5F7)
val DramaBackgroundTertiary   = Color(0xFFE8EBF0)
val DramaBackgroundPrimary    = Color(0xFFFFFFFF)
val DramaBackgroundOnPrimary  = Color(0xFF000000)
val DramaBackgroundPopup01    = Color(0xFFFFFFFF)
val DramaBackgroundPopup02    = Color(0xFFFFFFFF)
val DramaBackgroundOnPopup02  = Color(0xFFF3F5F7)

// ── Background（Dark）──────────────────────────────────────────
val DramaBackgroundDefaultDark    = Color(0xFF000000)
val DramaBackgroundSecondaryDark  = Color(0xFF1A1A1A)
val DramaBackgroundTertiaryDark   = Color(0xFF333333)
val DramaBackgroundPrimaryDark    = Color(0xFF000000)
val DramaBackgroundOnPrimaryDark  = Color(0xFFFFFFFF)
val DramaBackgroundPopup01Dark    = Color(0xFF202020)
val DramaBackgroundPopup02Dark    = Color(0xFF111111)
val DramaBackgroundOnPopup02Dark  = Color(0xFF252525)

// ── New Background（Light）─────────────────────────────────────
val DramaNewBackgroundPopup   = Color(0xFFFFFFFF)
val DramaNewBackgroundPage1   = Color(0xFFFFFFFF)
val DramaNewBackgroundPage2   = Color(0xFFF3F4F8)
val DramaNewBackgroundCard    = Color(0xFFFFFFFF)
val DramaNewBackgroundGray700 = Color(0x0F161823)
val DramaNewBackgroundGray800 = Color(0x0F161823)
val DramaNewBackgroundGray900 = Color(0x1A161823)

// ── New Background（Dark）──────────────────────────────────────
val DramaNewBackgroundPopupDark   = Color(0xFF262627)
val DramaNewBackgroundPage1Dark   = Color(0xFF000000)
val DramaNewBackgroundPage2Dark   = Color(0xFF000000)
val DramaNewBackgroundCardDark    = Color(0xFF19191A)
val DramaNewBackgroundGray700Dark = Color(0x14FFFFFF)
val DramaNewBackgroundGray800Dark = Color(0x1EFFFFFF)
val DramaNewBackgroundGray900Dark = Color(0x29FFFFFF)

// ── New Divider（Light/Dark）───────────────────────────────────
val DramaNewDivider01     = Color(0x1F161823)
val DramaNewDivider01Dark = Color(0x29FFFFFF)

// ── Opacity（Light）────────────────────────────────────────────
val DramaOpacityDivider  = Color(0x1A000000)
val DramaOpacityDivider2 = Color(0x14000000)

// ── Opacity（Dark）─────────────────────────────────────────────
val DramaOpacityDividerDark  = Color(0x26FFFFFF)
val DramaOpacityDivider2Dark = Color(0x14FFFFFF)

// ── Mask（Light/Dark 共用，同色）────────────────────────────────
val DramaOpacityMask = Color(0xB3000000)

// ── Membership（Light/Dark 共用，同色）────────────────────────
val DramaMembershipText   = Color(0xFF5D3100)
val DramaMembership01     = Color(0xFFFF3FBC)
val DramaMembership03     = Color(0xFF5D3100)

// ── Membership（Light）─────────────────────────────────────────
val DramaMembershipTextGold = Color(0xFFAD8132)

// ── Membership（Dark）──────────────────────────────────────────
val DramaMembershipTextGoldDark = Color(0xFFFFE4AC)
val DramaMembership02Dark       = Color(0xFFB697A5)

val Drama_BF000000      = Color(0xBF000000)
