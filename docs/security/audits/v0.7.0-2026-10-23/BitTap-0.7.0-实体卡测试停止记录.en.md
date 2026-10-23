[简体中文](BitTap-0.7.0-实体卡测试停止记录.md) · [English](BitTap-0.7.0-实体卡测试停止记录.en.md) · [文档目录 / Documentation](../../../README.md)

> Public translation: technical conclusions and limitations retained. Local evidence paths are archive indexes, not files distributed here. [Latest overall status](README.en.md).

# BitTap v0.7.0 physical-card testing stop record

2026-10-23: user explicitly stopped physical-card tests. No permanent lock was performed; no further card reads/writes.

- Release app completed an actual write and readback comparison.
- After the first failure, read-only inspection reported NTAG215, 492-byte capacity, writable, lock bits unset and empty NDEF; subsequent rewrite/app readback passed.
- During deliberate removal, app reported failure/interruption rather than success. Two subsequent reads returned another Type 4 read-only object (4-byte UID, capacity 348), inconsistent with the original NTAG215. Original card's post-interruption final contents were not verified and recovery closure was incomplete.
- **Write/interruption/permanent-lock acceptance cannot collectively be marked passed. Lock was never performed. Original card's final contents were not reconfirmed.**

Installed release and two audit packages removed. Accessibility restored to saved original values and read back. Two phone test-wallet backups deleted only after hash matching; recovery materials privately retained by maintainer, excluded here and never to be funded.

Earlier software/host audit conclusions stand; remaining risks in main report. No production-code/remote-repository changes in this testing stage.
