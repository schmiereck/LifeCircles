# Single-energy compatibility fixture

`single-energy-v1.cells` was generated on 2026-10-17 from production commit
`55fdda6`, before adding `EnergyType` or the secondary store to `Cell`.
It is a Java-serialized `ArrayList<Cell>` with two real, serializable `CellBrain`s.

Expected primary energies: 0.375 and 0.8125. Ages: 42 and 7. First cell state: 5.
No secondary-energy field exists in this file's Cell descriptor.

SHA-256: `CDBEB963C9CCE04D1E496293AEFDE8DCA07EA8BC59BF24C878972F22B3F5841E`.

The regression must preserve old primary values and initialize new stores to zero.
Do not regenerate this fixture with a multi-energy version of Cell.
