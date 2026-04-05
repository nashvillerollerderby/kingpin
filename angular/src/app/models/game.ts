export interface Clock {
  Direction: boolean
  Id: string
  InvertedTime: number
  MaximumTime: number
  Name: string
  Number: number
  Readonly: boolean
  Running: boolean
  Time: number
}

export interface Clocks {
  Intermission?: Clock
  Jam?: Clock
  Lineup?: Clock
  Period?: Clock
  Timeout?: Clock
}

export interface Skater {
  Annotation: string
  BoxTripSymbols: string
  BoxTripSymbolsAfterSP: string
  BoxTripSymbolsBeforeSP: string
  Id: string
  NotFielded: boolean
  Number: number
  PenaltyBox: boolean
  Position: string
  Previous: string
  Readonly: boolean
  SitFor: boolean
  SkaterNumber: string
}

export interface Fielding {
  [key: string]: Skater
}

export interface ScoringTrip {
  AfterSP: boolean
  Annotation: string
  Current: boolean
  Duration: number
  Id: string
  JamClockEnd: number
  JamClockStart: number
  Next?: string
  Number: number
  Readonly: boolean
  Score: number
}

export interface TeamJam {
  AfterSPScore: number
  Calloff: boolean
  CurrentTrip: string
  CurrentTripNumber: number
  DisplayLead: boolean
  Fielding: Fielding
  Id: string
  Injury: boolean
  JamScore: number
  LastScore: number
  Lead: boolean
  Lost: boolean
  NoInitial: boolean
  NoPivot: boolean
  Number: number
  OsOffset: number
  OsOffsetReason: string
  Previous: string
  Readonly: boolean
  ScoringTrip: {
    [key: string]: ScoringTrip
  }
  StarPass: boolean
  TotalScore: number
}

export interface Jam {
  Duration: number
  Id: string
  InjuryContinuation: boolean
  Next?: string
  Number: number
  Overtime: boolean
  PeriodClockDisplayEnd: number
  PeriodClockElapsedEnd: number
  PeriodClockElapsedStart: number
  PeriodNumber: number
  Previous: string
  Readonly: boolean
  StarPass: boolean
  TeamJam: {
    [key: string]: TeamJam
  }
}

export interface Timeout {
  Duration: number
  Id: string
  OrRequest: string
  OrResult: string
  Owner: string
  PeriodClockElapsedEnd: number
  PeriodClockElapsedStart: number
  PeriodClockEnd: number
  PrecedingJamNumber: number
  Readonly: boolean
  RetainedReview: boolean
  Review: boolean
  Running: boolean
  WalltimeEnd: number
  WalltimeStart: number
}

export interface Period {
  CurrentJam: string
  CurrentJamNumber: number
  Duration: number
  Id: string
  LocalTimeStart: string
  Next?: string
  Number: number
  Readonly: boolean
  Running: boolean
  SuddenScoring: boolean
  Team: {
    PenaltyCount: number
    Points: number
  }
  Timeout: {
    [key: string]: Timeout
  }
  WalltimeEnd: number
  WalltimeStart: number
}

export interface TeamPositions {
  [key: string]: {
    Annotation: string
    CurrentBoxSymbols:string
    CurrentFielding: string
    CurrentPenalties: string
    ExtraPenaltyTime: number
    Flags: string
    HasUnserved: boolean
    Id: string
    Name: string
    PenaltyBox: boolean
    PenaltyCount: number
    PenaltyDetails: string
    Readonly: boolean
    RosterNumber: string
  }
}

export interface Team {
  ActiveScoreAdjustmentAmount: number
  AllBlockersSet: boolean
  Calloff: boolean
  CurrentTrip: string
  DisplayLead: boolean
  FieldingAdvancePending: boolean
  FileName: string
  FullName: string
  Id: string
  InOfficialReview: boolean
  InTimeout: boolean
  Initials: string
  Injury: boolean
  JamScore: number
  LastScore: number
  Lead: boolean
  LeagueName: string
  Logo: string
  Lost: boolean
  Name: string
  NoInitial: true
  NoPivot: boolean
  OfficialReviews: number
  Position: TeamPositions
  PreparedTeam: string
  PreparedTeamConnected: boolean
  Readonly: boolean
  RetainedOfficialReview: boolean
  RunningOrEndedTeamJam: string
  RunningOrUpcomingTeamJam: string
  Score: number
  StarPass: boolean
  TeamName: string
  Timeouts: number
  TotalPenalties: number
  TripScore: number
}

export interface Game {
  AbortReason?: string
  Clock: Clocks
  ClockDuringFinalScore?: boolean
  CurrentPeriod?: string
  CurrentPeriodNumber?: number
  CurrentTimeout?: string
  ExportBlockedBy?: string
  Filename?: string
  Id?: string
  InJam?: boolean
  InOvertime?: boolean
  InPeriod?: boolean
  InSuddenScoring?: boolean
  InjuryContinuationUpcoming?: boolean
  JsonExists?: boolean
  Jam: {
    [key: string]: Jam
  }
  Label?: any // TODO: set up label interface
  LastFileUpdate?: string
  Name?: string
  NameFormat?: string
  NoMoreJam?: boolean
  OfficialReview?: boolean
  OfficialScore?: boolean
  PenaltyCode?: any
  Period?: Period
  Readonly?: boolean
  Rule: any
  Ruleset?: string
  RulesetName?: string
  State?: string
  StatsbookExists?: boolean
  SuspensionsServed?: string
  Team: {
    [key: string]: Team
  }
  TimeoutOwner?: string
  UpcomingJam?: string
  UpcomingJamNumber?: number
  UpdateInProgress?: boolean
}