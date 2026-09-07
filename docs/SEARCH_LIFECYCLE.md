# Alumni Mentoring Portal: Search Page Layout & Request Lifecycle

## 1. Overview
The Alumni Search feature enables students and portal users to discover alumni mentors based on diverse criteria including name, current company, designation, department, skills, and industry experience.

To demonstrate fundamental algorithmic concepts alongside modern web engineering, the system implements **two custom search engines**:
1. **Manual Linear Search**: Sequential multi-field scanner capable of searching substrings across all profile attributes ($O(N)$ comparisons).
2. **Manual Binary Search**: Logarithmic divide-and-conquer search ($O(\log N)$ comparisons) operating on ordered attributes (such as `full_name`, `company`, `graduation_year`, or `experience_years`), capable of finding exact matches and contiguous matching ranges (prefixes or range boundaries).

---

## 2. Search Page Layout Architecture

The Search UI is laid out in a modern, intuitive visual hierarchy:

```
+-----------------------------------------------------------------------------------+
|  [Logo: AlumniConnect]             [Tab: Find Mentors]   [Tab: Register Alumni]   |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|                           EXPLORE ALUMNI MENTORS                                  |
|         Connect with experienced graduates across top tech & business firms       |
|                                                                                   |
|   +-------------------------------------------------------------+ +-----------+  |
|   | 🔍  Search by mentor name, company, skills, or role...     | |  Search   |  |
|   +-------------------------------------------------------------+ +-----------+  |
|                                                                                   |
|   [ Algorithm Mode:  (o) Linear Search [All Fields]  ( ) Binary Search [Sorted] ] |
|   [ Sort / Binary Key: Full Name | Company | Graduation Year | Experience       ] |
|                                                                                   |
|   Filters: [ Department: All v ] [ Industry: All v ] [ Exp: Any v ] [ Reset ]     |
+-----------------------------------------------------------------------------------+
|  ⚡ Search Metrics: 14 matches found in 0.38ms | 14 comparisons (Linear Mode)      |
+-----------------------------------------------------------------------------------+
|                                                                                   |
|  +-------------------------+  +-------------------------+  +--------------------+ |
|  | [Avatar] Anurag Patil   |  | [Avatar] Vishwesh B.    |  | [Avatar] Rohan M.  | |
|  | AI Engineer @ Microsoft |  | AI Engineer @ Microsoft |  | AI Engineer @ G... | |
|  | Comp Engg · 4 yrs exp   |  | Comp Engg · 4 yrs exp   |  | Comp Engg · 3 yrs  | |
|  | Tags: [Java] [Python]   |  | Tags: [Python] [C++]    |  | Tags: [AI] [OS]    | |
|  | [ View Profile / Connect|  | [ View Profile / Connect|  | [ View Profile...  | |
|  +-------------------------+  +-------------------------+  +--------------------+ |
|                                                                                   |
+-----------------------------------------------------------------------------------+
```

### Layout Components
1. **Global Header & Navigation**: Allows seamless switching between finding mentors and registering as an alumnus.
2. **Search Control Bar**:
   - Unified search input supporting instant typing (with debouncing) or enter key trigger.
   - **Algorithm Selector Toggle**: Lets users toggle between Linear Search and Binary Search to visualize algorithm behavior.
   - **Binary Key Selector**: When in Binary Search mode, specifies which sorted attribute key the logarithmic search is targeting (`full_name`, `company`, `graduation_year`, `experience_years`).
3. **Filter Panel**:
   - Multi-facet filters: Department, Industry, Minimum Experience, Graduation Year range.
   - Active filter tags and one-click "Reset All" button.
4. **Search Metrics Banner**:
   - Displays real-time algorithm execution stats: Algorithm used, total alumni dataset size, match count, comparison iterations executed, and execution time in milliseconds.
5. **Mentor Card Grid**:
   - Responsive multi-column grid displaying mentor avatars, roles, company tags, experience years, skills pills (clickable to search), bio snippet, and "Connect" CTA.
6. **Connect Modal**:
   - Displays full mentor profile, LinkedIn link, and direct contact details.

---

## 3. End-to-End Request Lifecycle

The request lifecycle traces the complete journey of a search operation from client input to server processing and UI rendering:

```mermaid
sequenceDiagram
    autonumber
    actor Student as Student / Client User
    participant UI as React SearchPage
    participant Debounce as Input Controller / Debouncer
    participant Router as Express Router (/api/mentors/search)
    participant Ctrl as Mentor Controller
    participant Repo as Alumni Repository
    participant Engine as Search Engine (Linear / Binary)
    participant DB as MySQL Database

    Student->>UI: Type search query or select filter
    UI->>Debounce: Queue input change (300ms debounce)
    Debounce->>Router: GET /api/mentors/search?query=Google&algorithm=linear&department=Computer
    Router->>Ctrl: handleSearchMentors(req, res)
    Ctrl->>Repo: getActiveAlumniPool()
    Repo->>DB: SELECT * FROM alumni JOIN users...
    DB-->>Repo: Return records
    Repo-->>Ctrl: Array of Alumni Entities
    
    alt Algorithm == 'linear'
        Ctrl->>Engine: manualLinearSearch(dataset, query, fields)
        Engine->>Engine: Loop i=0 to N-1; compare tokens across name, company, skills
        Engine-->>Ctrl: { matches, comparisons: N, executionTimeMs }
    else Algorithm == 'binary'
        Ctrl->>Engine: manualBinarySearch(dataset, key, query)
        Engine->>Engine: Verify/Sort by key; low=0, high=N-1, mid=(low+high)/2
        Engine->>Engine: Compare target; expand lower/upper bounds for matches
        Engine-->>Ctrl: { matches, comparisons: log2(N), executionTimeMs }
    end

    Ctrl->>Ctrl: Apply secondary facet filters (department, industry, experience)
    Ctrl-->>Router: HTTP 200 OK with JSON response
    Router-->>UI: { success: true, count, metrics: {...}, data: [...] }
    UI->>UI: Set results in state, update comparison stats, stop skeleton loading
    UI-->>Student: Render interactive mentor cards & performance badges
```

### Lifecycle Stages in Detail

| Stage | Component | Description |
|---|---|---|
| **1. User Interaction** | React Frontend | User types in search bar, toggles search algorithm, or selects department/experience filters. |
| **2. Request Dispatch** | Fetch API / Axios | Sends HTTP `GET /api/mentors/search` with URL parameters (`query`, `algorithm`, `key`, `department`, `industry`, `minExp`). |
| **3. Request Routing & Validation** | Express Router & Controller | Validates query parameters; ensures valid algorithm (`linear` or `binary`) and sanitizes string inputs. |
| **4. Data Fetching** | Alumni Repository | Queries MySQL `users` and `alumni` tables, normalizing skills and profile data. |
| **5. Algorithm Execution** | Custom Search Engine | Runs either the manual Linear Search or manual Binary Search algorithm. Records comparison operations and elapsed time. |
| **6. Filter Post-Processing** | Mentor Controller | Applies any facet constraints (e.g. department or industry match) to the candidate result set. |
| **7. Response Delivery** | Express Response | Sends structured JSON with search results and detailed execution telemetry. |
| **8. UI Hydration** | React Virtual DOM | Updates results state, renders mentor cards, shows active algorithm comparison badges. |

---

## 4. Algorithm Complexity Analysis

| Metric | Manual Linear Search | Manual Binary Search |
|---|---|---|
| **Best Case Time** | $O(1)$ (target is first element) | $O(1)$ (target is at exact middle) |
| **Worst Case Time** | $O(N)$ (must inspect all $N$ elements) | $O(\log N + K)$ ($K$ = matching prefix range) |
| **Average Case Time** | $O(N)$ | $O(\log N)$ |
| **Space Complexity** | $O(1)$ auxiliary | $O(1)$ auxiliary |
| **Data Requirement** | Unsorted or sorted array | **Strictly sorted** by target search key |
| **Search Scope** | Multi-attribute substring & token matching | Exact match & prefix match on sorted key |
| **Typical Comparisons (N=1000)** | ~1,000 comparisons | ~10 comparisons |
