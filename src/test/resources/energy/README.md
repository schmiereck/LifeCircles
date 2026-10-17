# Single-energy compatibility fixture

`single-energy-v1.cells` was generated on 2026-10-17 from production commit
`55fdda6`, before adding `EnergyType` or the secondary store to `Cell`.
It is a Java-serialized `ArrayList<Cell>` with two real, serializable `CellBrain`s.

Expected primary energies: 0.375 and 0.8125. Ages: 42 and 7. First cell state: 5.
No secondary-energy field exists in this file's Cell descriptor.

SHA-256: `CDBEB963C9CCE04D1E496293AEFDE8DCA07EA8BC59BF24C878972F22B3F5841E`.

The regression must preserve old primary values and initialize new stores to zero.
Do not regenerate this fixture with a multi-energy version of Cell.

## Single-conversion compatibility fixture

`single-conversion-v1.cells` was generated on 2026-10-17 with `Cell` from production
commit `adb7158`, before specialization-dependent conversion programs. The old Cell
source was compiled in `target/single-enzyme-fixture` against the pre-change production
classes and used first on the fixture generator's classpath. No working source or GUI
process was replaced. It is a Java-serialized `ArrayList<Cell>` with two real brains.
Its Cell descriptor has `energyConversion`, but no `energyConversionProgram` field.

- First: specialization 5, primary 0.2, energy 111 = 0.6, enzyme 111 → 000 at capacity 0.37.
- Second: specialization 2, primary 0.4, energy 001 = 0.3, enzyme 001 → 010 at capacity 0.8.

SHA-256: `2C9C3B349F027BD6D32B3BC0B70F87B07228C529E5A95D75354FC7974BE45C90`.

The regression checks that each former enzyme remains available with unchanged
parameters in all eight specializations. Do not regenerate this fixture with the
specialization-program version of Cell.
