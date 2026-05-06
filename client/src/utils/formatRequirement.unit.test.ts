import { describe, expect, it } from "vitest";
import { formatRequirement } from "./formatRequirement";

describe("formatRequirement", () => {
  it("formats nested AND/OR prerequisite trees", () => {
    expect(
      formatRequirement({
        type: "AND",
        children: [
          { type: "COURSE", requiredCourseCode: "COMPSCI 187" },
          {
            type: "OR",
            children: [
              { type: "COURSE", requiredCourseCode: "MATH 131" },
              { type: "COURSE", requiredCourseCode: "MATH 132" },
            ],
          },
        ],
      }),
    ).toBe("COMPSCI 187 AND (MATH 131 OR MATH 132)");
  });
});
