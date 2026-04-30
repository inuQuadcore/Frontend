package com.everybuddy.app.ui.theme

import androidx.compose.ui.graphics.Color


// Main Color #0167FF / Black #404040 / Gray1 #8F9399
// Gray2 #E4E6EC / White #FDFDFD
// 구분선 #EDEDED

/** 메인 블루 — 로그인 버튼, 강조 요소 */
val EBBlue  = Color(0xFF0167FF)

/** 텍스트 기본색 */
val EBBlack = Color(0xFF404040)

/** 화이트 */
val EBWhite = Color(0xFFFDFDFD)

/** Gray1 — placeholder, 비활성 아이콘 */
val EBGray1 = Color(0xFF8F9399)

/** Gray2 — 비활성 버튼 배경, 구분선 */
val EBGray2 = Color(0xFFE4E6EC)

/** Gray3 — 입력 필드 배경 */
val EBGray3 = Color(0xFFFFFFFF)

/** 에러 — 빨간 에러 텍스트 */
val EBError = Color(0xFFFF2B01)

// ── 시맨틱 컬러 (컴포넌트 바인딩용) ──────────────────

/** 로고 border — Figma: border(width=2.dp, color=Col…) */
val LogoBorderColor     = EBBlack

/** 활성 로그인 버튼 배경 */
val ButtonPrimaryBg     = EBBlue

/** 비활성/구글 버튼 배경 */
val ButtonSecondaryBg   = EBGray2

/** 입력 필드 배경 */
val TextFieldBg         = EBGray3

/** 입력 필드 포커스 테두리 */
val TextFieldBorderFocus = EBGray1
