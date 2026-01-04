#!/bin/bash

# Pre-commit hook: 커밋 메시지에 이슈 번호 검증
# 형식: "type: message #이슈번호" 또는 "type: message (#이슈번호)"
# 예시: "feat: 청구 생성 API 추가 #5", "fix: 버그 수정 (#12)"

COMMIT_MSG_FILE=$1
COMMIT_MSG=$(cat "$COMMIT_MSG_FILE")

# 이슈 번호 패턴: #숫자 또는 (#숫자)
ISSUE_PATTERN='#[0-9]+'

# 커밋 메시지가 merge, revert 등 자동 생성된 경우 검증 스킵
if [[ "$COMMIT_MSG" =~ ^(Merge|Revert|Initial) ]]; then
    exit 0
fi

# 이슈 번호 검증
if ! echo "$COMMIT_MSG" | grep -qE "$ISSUE_PATTERN"; then
    echo "❌ 커밋 메시지 검증 실패"
    echo ""
    echo "커밋 메시지에 이슈 번호가 필요합니다."
    echo ""
    echo "올바른 형식:"
    echo "  feat: 청구 생성 API 추가 #5"
    echo "  fix: 버그 수정 (#12)"
    echo "  docs: 문서 업데이트 #3"
    echo ""
    echo "현재 커밋 메시지:"
    echo "  $COMMIT_MSG"
    echo ""
    exit 1
fi

# 커밋 타입 검증 (선택적)
VALID_TYPES="feat|fix|docs|refactor|test|chore"
if ! echo "$COMMIT_MSG" | grep -qE "^($VALID_TYPES):"; then
    echo "⚠️  경고: 권장 커밋 타입을 사용하지 않았습니다."
    echo "권장 타입: feat, fix, docs, refactor, test, chore"
    echo ""
    # 경고만 하고 통과
fi

echo "✅ 커밋 메시지 검증 통과"
exit 0
