import { describe, expect, it } from "vitest";
import { mapCourseDetail } from "./courseMappers";

describe("mapCourseDetail", () => {
  it("maps nested requirement DTOs into frontend prerequisite trees", () => {
    expect(
      mapCourseDetail({
        courseCode: "COMPSCI 220",
        title: "Programming Methodology",
        credits: 4,
        description: "desc",
        courseType: "CS",
        prerequisiteDescription: "COMPSCI 187 AND (MATH 131 OR MATH 132)",
        prerequisite: {
          type: "AND",
          courseCode: null,
          children: [
            { type: "COURSE", courseCode: "COMPSCI 187", children: [] },
            {
              type: "OR",
              courseCode: null,
              children: [
                { type: "COURSE", courseCode: "MATH 131", children: [] },
                { type: "COURSE", courseCode: "MATH 132", children: [] },
              ],
            },
          ],
        },
      }).prerequisite,
    ).toEqual({
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
    });
  });

  it("normalizes nullable course detail fields for the frontend model", () => {
    expect(
      mapCourseDetail({
        courseCode: "SPECIAL 190",
        title: "Special Topics",
        credits: null,
        description: null,
        courseType: "OTHER",
        prerequisite: null,
        prerequisiteDescription: null,
      }),
    ).toMatchObject({
      courseCode: "SPECIAL 190",
      credits: 0,
      description: "",
      prerequisite: null,
    });
  });
});
