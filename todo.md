**QSpec Testing Framework**:

- Check that file/folder contains QSpec functions and can be executed (maybe ignore folders?)
- Auto-import QSpec lib when adding QSpec to the project/detect it/ask about?
- Grammar analysis:
    - Show suggestions inside .tst.desc: after/before/should, all asserts. Inject into ElementScope and ElementContext.
      Maybe even two types: QSPEC_DESC and QSPEC_EXPECT
    - 'mock' should be treated as 'set'