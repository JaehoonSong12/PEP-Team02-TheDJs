<!-- SUPPLEMENT: Grouped topics for State Management & Reactivity -->

# Signals & Reactivity

## Overview of Signals

**Signals** are a new way to manage reactive state in Angular. They provide a way to tell Angular exactly which parts of the UI need to change when a piece of data updates. 

In traditional Angular, the framework uses a process called "Change Detection" to check the entire component tree to see if anything has changed. While efficient, this can become a bottleneck in massive applications. **Signals** change this by providing "Fine-Grained Reactivity"—instead of checking everything, Angular knows exactly which specific component (and even which specific part of a template) depends on a particular signal.

### The Core Concept: The "Producer-Consumer" Model
To understand Signals, think of them as a live connection between two parties:

1.  **The Producer (The Signal):** A container for a value. When the value inside the container changes, the producer "notifies" everyone watching it.
2.  **The Consumer (The Effect/Template):** Any part of your code that "reads" the signal. These consumers "subscribe" to the producer automatically.

## The Three Pillars of Signals

Angular provides three fundamental primitives to build reactive systems.

### 1. Writable Signals (`signal`)
A Writable Signal is a value that you can directly change. It is the "Source of Truth."

*   **Creation:** `const count = signal(0);`
*   **Reading:** You **must** call the signal as a function to get its value: `count()`.
*   **Updating:**
    *   `.set(newValue)`: Replaces the value entirely.
    **`.update(fn)`**: Updates the value based on the current value (e.g., `count.update(c => c + 1)`).

```typescript
import { signal } from '@angular/core';

const count = signal(0);

// Reading the value
console.log(count()); // 0

// Setting a new value
count.set(5);

// Updating based on previous value
count.update(val => val + 1); // 6
```

#### Implementation Context: Writable Signals in Components

Signals provide a modern, reactive approach to state management directly within component classes. The `signal` function can be used to instantiate state variables that the HTML template can react to seamlessly.

```typescript
// File: angular1-example/src/app/app.ts
import { Component, signal } from '@angular/core';

@Component({
  // ...
})
export class App {
  // A read-only signal that provides a constant reactive value.
  readonly message = signal("but you should really do this");

  // A writable signal used to control UI state (e.g., button disablement).
  isDisabled = signal(true);

  toggleDisabledButton(){
    // Updating the signal via .set() based on its current evaluated value via isDisabled().
    this.isDisabled.set(!this.isDisabled());
  }
}
```

#### Implementation Context: Writable Signals for UI State
*(Extracted from `@angular3-demo/src/app/home/home.ts`)*

This component uses writable signals to manage the list of books, error messages, and tracking which book is currently being edited. This allows the template to react instantly when the data changes, without relying on legacy RxJS patterns.

- `books`: The list of books fetched from the API.
- `errorMessage`: User-facing error message displayed when an operation fails.
- `editingBookId`: Tracks which book is currently being edited (`null` = form is in "create" mode; a string ID = form is in "update" mode).

```typescript
import { Component, inject, OnInit, signal } from '@angular/core';
import { BookService } from '../book/book.service';
import { Book } from '../book/book.model';

@Component({
  // ...
})
export class Home implements OnInit {
  private bookService = inject(BookService);

  // --- Component state (signals) ---
  books = signal<Book[]>([]);
  errorMessage = signal('');
  editingBookId = signal<string | null>(null);

  ngOnInit(): void {
    this.loadBooks();
  }

  loadBooks(): void {
    this.bookService.getAll().subscribe({
      next: (books) => this.books.set(books),
      error: () => this.errorMessage.set('Failed to load books.'),
    });
  }
}
```

### 2. Computed Signals (`computed`)
A Computed Signal is a **read-only** signal that is derived from other signals. It is "reactive" because it automatically re-calculates whenever its dependencies change.

*   **Automatic Dependency Tracking:** You don't have to tell a computed signal which other signals it depends on; it figures it out the moment you read them inside the function.
*   **Memoization (Caching):** Computed signals are extremely efficient. They only re-calculate if their dependencies change. If you read a computed signal twice without the dependencies changing, it simply returns the cached value.

```typescript
import { signal, computed } from '@angular/core';

const count = signal(10);
const doubleCount = computed(() => count() * 2);

console.log(doubleCount()); // 20

count.set(20);
console.log(doubleCount()); // 40 (Automatically updated!)
```

#### Implementation Context: Computed Signals and Persistence
*(Extracted from `@angular3-demo/src/app/auth/auth.service.ts`)*

This service leverages `computed()` to derive a boolean authentication state (`isAuthenticated`) entirely from the raw JWT `token` signal. 

- `token`: A Signal holding the current JWT (`null` when not authenticated). It is initialized from `localStorage` so a returning user stays logged in.
- `isAuthenticated`: A derived signal that automatically updates whenever `token` changes. Components and guards can read this reactively without manual subscriptions.

```typescript
import { computed, Injectable, signal } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class AuthService {
  token = signal<string | null>(this.getStoredToken());
  isAuthenticated = computed(() => this.token() !== null);

  setToken(token: string): void {
    this.token.set(token);
    localStorage.setItem('auth_token', token);
  }

  logout(): void {
    this.token.set(null);
    localStorage.removeItem('auth_token');
  }

  private getStoredToken(): string | null {
    return localStorage.getItem('auth_token');
  }
}
```

### 3. Effects (`effect`)
An `effect` is a function that runs whenever one or more of the signals it reads change. While computed signals are for **calculating values**, effects are for **performing side effects**.

*   **Common Use Cases:** Logging to the console, synchronizing with LocalStorage, or manually manipulating the DOM (though this should be a last resort).
*   **Updating based on previous value:** Use `.update()` and pass an arrow function.
*   **Important Rule:** You should generally **not** try to change a signal's value inside an effect. This can lead to infinite loops.

> **Project Context:** The `.mutate()` function for signals was deprecated in Angular v17 and is explicitly **banned** per the `AGENTS.md` guidelines. All signal updates must utilize `.set()` or `.update()`.

```typescript
import { signal, effect } from '@angular/core';

const count = signal(0);

effect(() => {
  console.log(`The current count is: ${count()}`);
});

count.set(5); // Console logs: "The current count is: 5"
```

## Signals vs. RxJS: When to use which?

This is a common point of confusion. While they both handle reactivity, they are designed for different purposes.

| Feature | Signals | RxJS (Observables) |
| :--- | :--- | :--- |
| **Primary Goal** | **State Management.** Representing "what the value is right now." | **Event Orchestration.** Representing "a stream of events over time." |
| **Complexity** | Simple, synchronous, and easy to read. | Complex, asynchronous, and powerful. |
| **Logic** | Best for: `user.name`, `isLoggedIn`, `cartTotal`. | Best for: HTTP requests, WebSockets, Debounced search inputs. |
| **Subscription** | Automatic (implicit). | Manual (requires `.subscribe()` or `async` pipe). |

**The Modern Rule of Thumb:**
*   Use **Signals** for your component state and data that is displayed in the UI.
*   Use **RxJS** for asynchronous "streams" (API calls, timers, user input events) and complex data transformations.
*   **The Bridge:** In a professional app, you will often convert an RxJS Observable into a Signal (using `toSignal()`) so that your UI can consume it easily.

#### Implementation Context: The `toSignal()` Bridge
*(Extracted from `@angular2-pokedex/src/app/components/poke-sprite/poke-sprite.ts`)*

The `toSignal` function seamlessly converts an Observable into a Signal. However, because Observables are asynchronous and Signals are synchronous, the initial value of the resulting Signal might be `undefined` before the Observable emits its first value. To safely consume this in TypeScript, you must use **Type Narrowing**.

```typescript
import { Component, inject, ChangeDetectionStrategy } from '@angular/core';
import { toSignal } from '@angular/core/rxjs-interop';
import { PokeService } from '../../services/poke-service';
import { Pokemon } from '../../interfaces/pokemon';

@Component({
  selector: 'app-poke-sprite',
  templateUrl: './poke-sprite.html',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PokeSprite {
  private pokeService = inject(PokeService);

  // 1. The Bridge: Convert the Observable stream into a synchronous Signal
  pokemonData = toSignal(this.pokeService.pokemon$);

  getSpriteUrls() {
    // 2. Type Narrowing: We MUST specify 'as Pokemon' to prevent TypeScript errors,
    // because toSignal() can initially return undefined before the HTTP request completes.
    const narrowedPokemonData = this.pokemonData() as Pokemon; 
    
    const spriteURL = [];
    for (const spriteType in narrowedPokemonData.sprites) { 
      const narrowedSpriteType = spriteType as "back_default" | "front_default";
      if (narrowedSpriteType == "back_default" || narrowedSpriteType == 'front_default') {
        spriteURL.push(narrowedPokemonData.sprites[narrowedSpriteType]);
      }
    }
    return spriteURL;
  }
}
```

> [!NOTE] 
> Verified against official documentation: https://angular.dev/guide/signals/rxjs-interop (The initial value of a `toSignal` without `requireSync` is indeed `T | undefined`, necessitating type narrowing).

## Summary Table: Signal Operations

| Operation | Method | Description |
| :--- | :--- | :--- |
| **Create** | `signal(init)` | Creates a new writable signal. |
| **Read** | `mySignal()` | Accesses the current value. |
| **Replace** | `.set(val)` | Overwrites the value. |
| **Transform** | `.update(fn)` | Updates value based on current value. |
| **Derive** | `computed(() => ...)` | Creates a read-only, derived signal. |
| **React** | `effect(() => ...)` | Runs code when signals change. |

---

---

# RxJS & Observables

## Overview of Reactive Programming

Most programming is **imperative**: you tell the computer *step-by-step* what to do (e.g., "Get this data, then assign it to this variable"). 

**Reactive Programming** (via RxJS) is **declarative**: you describe the *flow of data* as it moves through a system. Instead of dealing with single values, you deal with **Streams**.

### The Stream Analogy: The Water Pipe
Imagine a water pipe running through a house:
*   **The Observable** is the pipe itself. It is the structure that carries the water.
*   **The Data (Value)** is the water flowing through the pipe.
*   **The Operators** are the filters, valves, or heaters attached to the pipe. They can clean the water, slow it down, or change its temperature before it reaches the faucet.
*   **The Observer (Subscriber)** is the person at the faucet. Nothing happens until someone actually "turns on the tap" (subscribes).

## 1. Observables: The Stream

An **Observable** is a blueprint for a stream of data. It is "lazy," meaning it does nothing until someone explicitly asks to listen to it.

### Key Characteristics
*   **Emit Multiple Values:** Unlike a Promise (which returns one value and finishes), an Observable can emit zero, one, or an infinite number of values over time.
*   **Lazy Execution:** An Observable won't start "running" its logic until a `.subscribe()` is called.
*   **Cancellable:** You can "turn off the tap" by unsubscribing, which stops the stream and prevents memory leaks.

```typescript
import { Observable } from 'rxjs';

const stream$ = new Observable(subscriber => {
  subscriber.next('First drop');
  subscriber.next('Second drop');
  setTimeout(() => {
    subscriber.next('Third drop (delayed)');
    subscriber.complete(); // The stream is now finished
  }, 2000);
});

// Nothing happens until we subscribe
const subscription = stream$.subscribe(val => console.log(val));

// If we wanted to stop listening early:
// subscription.unsubscribe();
```
*Note: In Angular, it is a convention to end variable names of Observables with a `$` (e.g., `data$`) to signal to other developers that the variable is a stream.*

---

## 2. Operators: The Pipe Filters

**Operators** are functions that allow you to manipulate, filter, or combine streams. You use the `.pipe()` method to chain them together.

### Common Categories of Operators

| Category | Operators | Purpose |
| :--- | :--- | :--- |
| **Transformation** | `map`, `scan` | Change the data (e.g., turn a number into a string). |
| **Filtering** | `filter`, `debounceTime`, `distinctUntilChanged` | Decide which values are allowed to pass through. |
| **Flattening** | `switchMap`, `mergeMap`, `concatMap` | Handle "nested" observables (e.g., an API call triggered by a click). |
| **Combination** | `combineLatest`, `forkJoin`, `withLatestFrom` | Merge multiple streams into one. |

### Example: The Search Input Pattern
This is the most common real-world use of RxJS: taking a user's keystrokes, waiting for them to stop typing, and then making an API call.

```typescript
import { debounceTime, distinctUntilChanged, switchMap } from 'rxjs/operators';

// userSearch$ is an observable of every keystroke in an input field
userSearch$.pipe(
  debounceTime(300),          // Wait for 300ms of silence (don't spam the API)
  distinctUntilChanged(),     // Only proceed if the text actually changed
  switchMap(term => this.api.search(term)) // Cancel previous search and start new one
).subscribe(results => {
  this.searchResults = results;
});
```

---

## 3. Subjects: The Multicasters

A standard Observable is **unicast**: every time you subscribe, the "pipe" starts from scratch for that specific person. If you have 10 subscribers, the logic runs 10 times.

A **Subject** is a special type of Observable that is also an **Observer**. This makes it **multicast**: it acts as a central hub that broadcasts the same data to all current subscribers simultaneously.

### The Four Types of Subjects

| Type | Behavior | Analogy |
| :--- | :--- | :--- |
| **`Subject`** | Emits only new values to current subscribers. | A Live Radio Broadcast (if you tune in late, you missed the beginning). |
| **`BehaviorSubject`** | Emits the **latest value** immediately to new subscribers. Requires an initial value. | A Digital Thermometer (you can walk up and see the current temp immediately). |
| **`ReplaySubject`** | "Replays" a specific number of previous values to new subscribers. | A YouTube Video (you can go back and see what happened earlier). |
| **`AsyncSubject`** | Only emits the **final value**, and only when the stream completes. | A Final Scoreboard (you only see the result once the game is over). |

**When to use `BehaviorSubject`:** This is the most common Subject used in Angular services to hold "State" (like the current logged-in user).

---

## 4. Consuming Streams: The `async` Pipe

In Angular, you have two ways to handle an Observable in a template: the "Manual Way" and the "Async Pipe Way."

### The Manual Way (Avoid this if possible)
You subscribe in the TypeScript class and assign the result to a local variable.
*   **The Danger:** You **must** remember to `unsubscribe()` in `ngOnDestroy`. If you forget, the subscription stays alive in the background, causing **memory leaks**.

### The `async` Pipe Way (The Gold Standard)
The `async` pipe is a built-in Angular tool that handles the entire lifecycle of an Observable for you.

#### Implementation Context: Async Pipe Aliasing (`as`)
*(Extracted from `@angular2-pokedex/src/app/components/poke-name/poke-name.html`)*

When you need to read multiple properties from an observable object, unwrapping it multiple times (e.g., `(stream$ | async)?.name`) is inefficient and prone to null-reference errors. Instead, use the `; as aliasName` syntax within modern `@if` blocks to unwrap it once and bind it to a local template variable safely.

```html
<!-- 1. Safely unwrap the observable stream and alias the result as 'pokemon' -->
@if (this.pokeService.pokemon$ | async; as pokemon) {
    <!-- 2. The 'pokemon' alias is now evaluated safely, preventing null errors -->
    @if (pokemon.name != '') {
        <h2>A wild {{pokemon.name}} has appeared!</h2>
    }
}
```

> [!NOTE] 
> Verified against official documentation: https://angular.dev/guide/templates/control-flow (The `; as aliasName` syntax is the official standard for unwrapping async pipes within modern `@if` blocks).

**Why the `async` pipe is superior:**
1.  **Memory Safety:** Zero risk of memory leaks.
2.  **Simplicity:** No boilerplate code in your `.ts` file.
3.  **Change Detection:** It tells Angular exactly when a new value has arrived, making it highly efficient.

### Summary: Manual vs. Async Pipe

| Feature | Manual `.subscribe()` | `async` Pipe |
| :--- | :--- | :--- |
| **Subscription** | Manual (`.subscribe()`) | Automatic |
| **Unsubscription** | **Manual (Required!)** | **Automatic (Built-in)** |
| **Complexity** | High (Boilerplate heavy) | Low (Declarative) |
| **Risk** | High (Memory Leaks) | Very Low |

## Real-World Case Study: Multicasting via PokeAPI

**The Scenario:** We want to fetch data for a specific Pokémon (e.g., Pikachu) and display its **Name** in a Header component and its **Types** in a Stats component. We want to fetch the data **only once** and share that same data with both components.

### 1. The Service (The Hub)
The service handles the HTTP request and uses a `BehaviorSubject` to "hold" the Pokémon data and broadcast it to anyone listening.

#### Implementation Context: Service State Encapsulation
*(Extracted from `@angular2-pokedex/src/app/services/poke-service.ts`)*
```typescript
import { HttpClient } from '@angular/common/http';
import { inject, Injectable } from '@angular/core';
import { BehaviorSubject, catchError, Observable, of, tap } from 'rxjs';

@Injectable({ providedIn: 'root' })
export class PokeService {
  private pokeAPIUrl = "https://pokeapi.co/api/v2/pokemon/"
  private httpClient = inject(HttpClient)

  // 1. The "Private" Source: Internal storage for Pokémon data.
  private pokemonSubject: BehaviorSubject<Pokemon>

  // 2. The "Public" Stream: Components subscribe to this Observable.
  pokemon$: Observable<Pokemon>;

  private emptyPokemon = { name: '', sprites: { back_default:'', front_default:'' } }

  constructor(){
    this.pokemonSubject = new BehaviorSubject<Pokemon>(this.emptyPokemon);
    this.pokemon$ = this.pokemonSubject.asObservable();
  }

  queryPokemon(identifier: string){
    this.httpClient.get<Pokemon>(this.pokeAPIUrl + identifier)
      .pipe(
        // 3. The "Tap": Update our subject with the new data
        tap(pokemonData => this.pokemonSubject.next(pokemonData)),
        // 4. Fault Tolerance: Reset state on error
        catchError(error => {
          console.log(`Something went wrong: ${error}`)
          this.pokemonSubject.next(this.emptyPokemon);
          return of(null);
        })
      ).subscribe() 
  }
}
```

### 2. The Trigger Component (The Requester)
*(Extracted from `@angular2-pokedex/src/app/components/poke-search/poke-search.ts`)*
This component initiates the process by calling the service method.

```typescript
import { Component, inject, ChangeDetectionStrategy } from '@angular/core';

@Component({
  selector: 'app-poke-search',
  changeDetection: ChangeDetectionStrategy.OnPush
})
export class PokeSearch {
  private pokeService = inject(PokeService);

  searchPokemon(name: string) {
    this.pokeService.queryPokemon(name);
  }
}
```

### 3. The Consumer Components (The Subscribers)
Both components subscribe to the **same** `pokemon$` stream. Because we used a `BehaviorSubject`, they both receive the exact same data at the same time.

**Component A: The Header**
```typescript
@Component({
  selector: 'app-pokemon-header',
  template: `<h1>Target: {{ (pokemon$ | async)?.name | uppercase }}</h1>`,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AsyncPipe, UpperCasePipe] // Note: AsyncPipe is required!
})
export class HeaderComponent {
  pokemon$ = inject(PokemonService).pokemon$;
}
```

**Component B: The Stats Display**
```typescript
@Component({
  selector: 'app-pokemon-stats',
  template: `
    @if (pokemon$ | async; as pokemon) {
      <ul>
        @for (t of pokemon.types; track t.type.name) {
          <li>Type: {{ t.type.name }}</li>
        }
      </ul>
    }
  `,
  changeDetection: ChangeDetectionStrategy.OnPush,
  imports: [AsyncPipe]
})
export class StatsComponent {
  pokemon$ = inject(PokemonService).pokemon$;
}
```

---

### Architectural Breakdown

| Layer | Role | Why this works |
| :--- | :--- | :--- |
| **`HttpClient`** | Fetches the data. | The primary way to get data from the web. |
| **`tap()`** | The "Side Effect." | It allows us to "peek" at the data coming through the pipe and send it to our Subject without breaking the stream. |
| **`BehaviorSubject`** | The Multicaster. | It acts as the "Single Source of Truth." It stores the data so that even if `StatsComponent` loads *after* the data was fetched, it still gets the current Pokémon immediately. |
| **`asObservable()`** | The "Shield." | This is a security best practice. It prevents components from accidentally calling `.next()` and "faking" the data; they can only listen, not speak. |
| **`async` Pipe** | The Consumer. | It handles the subscription to `pokemon$` and automatically cleans up, ensuring no memory leaks occur when the user navigates away. |

> [!NOTE]
> Verified against official documentation: https://angular.dev/guide/signals
> Verified `toSignal` interoperability: https://angular.dev/guide/signals/rxjs-interop
> Verified `computed` and `effect` best practices: https://angular.dev/guide/signals

