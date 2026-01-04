# Claude Code Hooks

이 디렉토리는 Claude Code가 특정 이벤트 발생 시 자동으로 실행하는 스크립트를 포함합니다.

## 설정된 Hooks

### pre-commit.sh
커밋 메시지에 이슈 번호가 포함되어 있는지 검증합니다.

**검증 규칙**:
- 커밋 메시지에 `#숫자` 형식의 이슈 번호 필수
- 예시: `feat: 청구 생성 API 추가 #5`
- 예시: `fix: 버그 수정 (#12)`

**통과하는 커밋 메시지**:
```
✅ feat: 청구 생성 API 추가 #5
✅ fix: 버그 수정 (#12)
✅ docs: 리드미 업데이트 #1 #2
```

**실패하는 커밋 메시지**:
```
❌ feat: 청구 생성 API 추가
❌ 청구 API 구현
```

**예외**:
- Merge, Revert, Initial로 시작하는 자동 생성 커밋은 검증 스킵

## Hook 작동 방식

1. Claude Code가 git 커밋을 생성할 때
2. 커밋 전에 `pre-commit.sh` 자동 실행
3. 검증 실패 시 커밋 중단되고 에러 메시지 표시
4. 검증 통과 시 커밋 진행

## 수동 테스트

```bash
# 테스트 커밋 메시지로 검증 시도
echo "feat: 테스트 기능" | .claude/hooks/pre-commit.sh /dev/stdin
# ❌ 실패 (이슈 번호 없음)

echo "feat: 테스트 기능 #123" | .claude/hooks/pre-commit.sh /dev/stdin
# ✅ 통과
```

## 비활성화

Hook을 임시로 비활성화하려면:
```bash
# 파일 이름 변경
mv .claude/hooks/pre-commit.sh .claude/hooks/pre-commit.sh.disabled
```
